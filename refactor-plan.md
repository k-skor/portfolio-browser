
# Refactor Plan
1. Change specific methods e.g. in FirestoreProjectRepository into classes with clear intent e.g. PagedSearch.
2. Separate and convert user flows implemented in OrbitStore into classes with clear intent, e.g. AccountMerger,
  GuestAccountConverter, ProfileCreation. Current example: SyncHandler (not yet clear and separate as desired).
