conn = new Mongo()
db = conn.getDB("sample")

db.artist.insert({ _id: 1, name: "AC/DC" })
db.artist.insert({ _id: 2, name: "Aerosmith" })
db.artist.insert({ _id: 3, name: "Alice In Chains" })

db.album.insert({ _id: 1, artist_id: 1, title: "Back In Black" })
db.album.insert({ _id: 2, artist_id: 1, title: "High Voltage" })
db.album.insert({ _id: 3, artist_id: 1, title: "For Those About To Rock We Salute You" })

db.album.insert({ _id: 4, artist_id: 2, title: "Get Your Wings" })
db.album.insert({ _id: 5, artist_id: 2, title: "Toys in the Attic" })
db.album.insert({ _id: 6, artist_id: 2, title: "Rocks" })

db.album.insert({ _id: 7, artist_id: 3, title: "Dirt" })
db.album.insert({ _id: 8, artist_id: 3, title: "The Best Of Alice In Chains" })
db.album.insert({ _id: 9, artist_id: 3, title: "Facelift" })
