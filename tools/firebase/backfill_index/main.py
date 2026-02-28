import logging
from polyglot.text import Text

from firebase_functions import https_fn, options
from firebase_functions.options import set_global_options
from firebase_functions.params import StringParam
from firebase_admin import initialize_app, firestore

from azure.identity import DefaultAzureCredential
from azure.search.documents import SearchClient

# For cost control, you can set the maximum number of containers that can be
# running at the same time. This helps mitigate the impact of unexpected
# traffic spikes by instead downgrading performance. This limit is a per-function
# limit. You can override the limit for each function using the max_instances
# parameter in the decorator, e.g. @https_fn.on_request(max_instances=5).
set_global_options(max_instances=10)

initialize_app()


BATCH_SIZE = 1000

@https_fn.on_request(
    region="eu-west3",
    timeout_sec=540,  # Maximize timeout (9 mins) for long running jobs
    memory=options.MemoryOption.MB_512
)
def backfill_azure_index(req: https_fn.Request) -> https_fn.Response:
    """
    HTTP Trigger to wipe/update the Azure Index with all data from Firestore.
    Usage: curl https://<region>-<project>.cloudfunctions.net/backfill_azure_index
    """

    body = req.get_json()
    user_id = body["userId"]

    if not user_id:
        return https_fn.Response("Missing userId", status=403)
    
    # 1. Lazy Load Configuration & Clients (Prevents global scope errors)
    endpoint = StringParam("AZURE_SEARCH_ENDPOINT")
    index_name = StringParam("INDEX_NAME")

    if not all([endpoint, index_name]):
        return https_fn.Response("Missing Environment Variables", status=500)

    try:
        search_client = SearchClient(
            endpoint=endpoint,
            index_name=index_name,
            credential=DefaultAzureCredential()
        )
        db = firestore.client()
        
        # 2. Iterate and Batch
        total_count = 0
        batch = []

        print(f"Reading from Firestore")
        # .stream() is crucial here! It reads one doc at a time, not all at once.
        user_ref = db.collection("users").document(user_id)
        docs = user_ref.collection("projects").stream()

        print("Attempting to backfill index")

        for doc in docs:
            data = doc.to_dict()

            lang_map = get_language_of(data["description"])
            
            # Map Firestore fields to Azure Index fields
            search_doc = {
                "id": doc.id,
                "name": data.get("name"),
                "description_pl": lang_map.get("pl"),
                "description_en": lang_map.get("en"),
                "@search.action": "mergeOrUpload" # Upsert (Create or Update)
            }
            
            batch.append(search_doc)

            # 3. Flush Batch if full
            if len(batch) >= BATCH_SIZE:
                _upload_batch(search_client, batch)
                total_count += len(batch)
                batch = [] # Reset

        # 4. Flush remaining documents
        if batch:
            _upload_batch(search_client, batch)
            total_count += len(batch)

        return https_fn.Response(f"Success! Indexed {total_count} documents.", status=200)

    except Exception as e:
        logging.error(f"Indexing failed: {str(e)}")
        return https_fn.Response(f"Error: {str(e)}", status=500)


def _upload_batch(client, documents):
    """Helper to handle the Azure upload safely"""
    try:
        results = client.upload_documents(documents=documents)
        
        # Optional: Check for individual failures in the batch
        if not all(r.succeeded for r in results):
            failed = [r for r in results if not r.succeeded]
            logging.warning(f"Batch had {len(failed)} failures.")
            
        logging.info(f"Uploaded batch of {len(documents)}")
    except Exception as e:
        # If a batch fails entirely (e.g. network blip), log it.
        # In production, you might want to retry here.
        logging.error(f"Batch upload crashed: {e}")
        raise e

def get_language_of(content):
    if not content:
        return dict()

    code = Text(content).language.code
    return { code: content }