from polyglot.text import Text

from firebase_functions import firestore_fn, options
from firebase_functions.options import set_global_options
from firebase_functions.params import StringParam
from firebase_admin import initialize_app
#from azure.core.credentials import AzureKeyCredentia

from azure.identity import DefaultAzureCredential
from azure.search.documents import SearchClient

# For cost control, you can set the maximum number of containers that can be
# running at the same time. This helps mitigate the impact of unexpected
# traffic spikes by instead downgrading performance. This limit is a per-function
# limit. You can override the limit for each function using the max_instances
# parameter in the decorator, e.g. @https_fn.on_request(max_instances=5).
set_global_options(max_instances=10)

initialize_app()


# Initialize Azure Client once (globally) to reuse connections
SEARCH_ENDPOINT = StringParam("AZURE_SEARCH_ENDPOINT")
#SEARCH_KEY = os.environ.get("AZURE_SEARCH_KEY")
index_name = StringParam("INDEX_NAME")

search_client = SearchClient(
    endpoint=SEARCH_ENDPOINT,
    index_name=index_name,
    credential=DefaultAzureCredential()
)

# Configuration: Which fields actually matter for search?
SEARCHABLE_FIELDS = ["name", "description"]

@firestore_fn.on_document_written(
    document="users/{userId}/projects/{projectId}",
    region="eu-west3", # Match your Firestore region
    memory=options.MemoryOption.MB_256 # Keep memory low for cost
)
def sync_to_azure(event: firestore_fn.Event[firestore_fn.Change]):
    
    # 1. Get Data Snapshots
    new_data = event.data.after.to_dict() if event.data.after else None
    old_data = event.data.before.to_dict() if event.data.before else None
    doc_id = event.params["projectId"]

    # --- SCENARIO A: DELETE ---
    # If new_data is None, the document was deleted.
    if new_data is None:
        print(f"Deleting {doc_id} from Azure...")
        # Azure Search allows deleting by ID even if doc doesn't exist
        search_client.delete_documents(documents=[{"id": doc_id}])
        return

    # --- SCENARIO B: CREATE (New Document) ---
    # If old_data is None, it's a brand new document.
    if old_data is None:
        print(f"Creating {doc_id} in Azure...")
        upload_to_azure(doc_id, new_data)
        return

    # --- SCENARIO C: UPDATE (The "Smart" Check) ---
    # Only update if a SEARCHABLE field has changed.
    needs_update = False
    for field in SEARCHABLE_FIELDS:
        if new_data.get(field) != old_data.get(field):
            needs_update = True
            break
            
    if needs_update:
        print(f"Significant change detected in {doc_id}. Updating Azure...")
        upload_to_azure(doc_id, new_data)
    else:
        print(f"Metadata update only for {doc_id}. Skipping Azure sync.")

def upload_to_azure(doc_id, data):
    # Construct payload - keep it minimal
    lang_map = get_language_of(data["description"])
    document = {
        "id": doc_id,
        "name": data.get("name"),
        "description_pl": lang_map.get("pl"),
        "description_en": lang_map.get("en"),
        "@search.action": "mergeOrUpload" 
    }
    
    # If using Vectors, you would generate embeddings here before uploading
    # document['vector'] = generate_embedding(data.get('description'))
    
    try:
        search_client.upload_documents(documents=[document])
    except Exception as e:
        print(f"Error uploading to Azure: {e}")

def get_language_of(content):
    if not content:
        return dict()

    code = Text(content).language.code
    return { code: content }