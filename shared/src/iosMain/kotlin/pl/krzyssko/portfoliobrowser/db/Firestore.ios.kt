package pl.krzyssko.portfoliobrowser.db


class IosFirestore: Firestore {
}

actual fun getFirestore(): Firestore = IosFirestore()
