package com.manager.gestionale;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

@RestController
public class ManagerController {
    String conn = "mongodb+srv://user:pswd@cluster0.u58z1in.mongodb.net/?retryWrites=true&w=majority";

    @PostMapping(value= "/addArticolo", consumes = "application/json", produces = "application/json")
    public Document addArticolo(@RequestBody Map<String, Object> body) {
        String connectionString = conn;
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("Esercitazioni");
                MongoCollection<Document> collection = database.getCollection("Inventory");
                Document filter = new Document("ArticoloID", body.get("ArticoloId"));
                Document update = new Document("$set", new Document()
                        .append("Giacenza", body.get("Giacenza"))
                        .append("Nome", body.get("Nome"))
                        .append("Tipologia", body.get("Tipologia")));
                Document doc = collection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                return doc;
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    @GetMapping(value="/getArticoli", produces = "application/json")
    public List<Document> getArticoli() {
        String connectionString = conn;
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("Esercitazioni");
                MongoCollection<Document> collection = database.getCollection("Inventory");
                FindIterable<Document> articoli = collection.find();
                List<Document> articoliList = new ArrayList<>();
                for (Document articolo : articoli) {
                    articoliList.add(articolo);
                }
                return articoliList;
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @PostMapping(value= "/addConnection", consumes = "application/json", produces = "application/json")
    public Document addConnection(@RequestBody Map<String, Object> body) {
        String connectionString = conn;
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("Esercitazioni");
                MongoCollection<Document> collection = database.getCollection("Connections");
                Document filter = new Document("LegameID", body.get("LegameID"));
                //update connections with connection interface members
                Document update = new Document("$set", new Document()
                        .append("LegameID", body.get("LegameID"))
                        .append("ArticoloID_padre", body.get("ArticoloID_padre"))
                        .append("ArticoloID_figlio", body.get("ArticoloID_figlio"))
                        .append("CoefficienteFabbisogno", body.get("CoefficienteFabbisogno")));
                Document doc = collection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                return doc;
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @PostMapping(value= "/addOrder", consumes = "application/json", produces = "application/json")
    public Document addOrder(@RequestBody Map<String, Object> body) {
        String connectionString = conn;
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("Esercitazioni");
                MongoCollection<Document> collection = database.getCollection("Orders");
                //retrieve the last order id
                Document last = collection.find().sort(new BasicDBObject("OrdineID", -1)).first();
                if(last == null) {
                    last = new Document("OrdineID", 0);
                }
                int lastOrderId =last.get("OrdineID", Integer.class);
                Document filter = new Document("OrdineID", lastOrderId+1);
                //check if the ArticleID correspond to an existing  article with Tipologia = "PF"
                Document article = database.getCollection("Inventory").find(eq("ArticoloID", Integer.parseInt((String)body.get("ArticoloID")))).first();
                if(article.getString("Tipologia").equals("PF")) {
                    Document update = new Document("$set", new Document()
                            .append("OrdineID", lastOrderId+1)
                            .append("ArticoloID", body.get("ArticoloID"))
                            .append("QuantitaDaProdurre", body.get("QuantitaDaProdurre"))
                            .append("ScaricoEffettuato", false));
                    Document doc = collection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                    return doc;
                }
                else {
                    System.out.println("L'articolo non è un prodotto finito");
                }
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @PostMapping(value= "/addFabbisogno", consumes = "application/json", produces = "application/json")
    public List<Document> addFabbisogno(@RequestBody Map<String, Object> body) {
        String connectionString = conn;
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("Esercitazioni");
                MongoCollection<Document> collection = database.getCollection("Fabbisogni");
                //retrive the order by order id from the body
                Document order = database.getCollection("Orders").find(eq("OrdineID",Integer.parseInt((String) body.get("OrdineID")))).first();
                assert order != null;
                String artId= order.getString("ArticoloID");
                int artIdInt = Integer.parseInt(artId);
                Document article = database.getCollection("Inventory").find(eq("ArticoloID", artIdInt)).first();
                FindIterable<Document> connection = database.getCollection("Connections").find(eq("ArticoloID_padre", artId));
                FindIterable<Document> AlreadyFab = database.getCollection("Fabbisogni").find(eq("OrdineId",(String)body.get("OrdineID")));
                List<Document> updatedDocuments = new ArrayList<>();
                if (AlreadyFab.iterator().hasNext()) {
                    return updatedDocuments;
                }

                Document last = collection.find().sort(new BasicDBObject("FabbisognoID", -1)).first();
                if(last == null) {
                    last = new Document("FabbisognoID", 0);
                }

                int lastFabbisognoId = last.getInteger("FabbisognoID");
                int articleId = article.getInteger("ArticoloID");
                int newQuantitaDaProdurre = 0;
                //check if i have already some Articles in the Inventory, so i have to produce less
                if(article.getInteger("Giacenza")!= 0) {
                    List<Document> list = new ArrayList<>();
                    newQuantitaDaProdurre = Integer.parseInt(order.getString("QuantitaDaProdurre")) - article.getInteger("Giacenza") ;
                    if(newQuantitaDaProdurre<0){
                        newQuantitaDaProdurre = 0;
                    }
                    int newGiacenzaInventory;
                    if(article.getInteger("Giacenza") -Integer.parseInt(order.getString("QuantitaDaProdurre"))<=0){
                        newGiacenzaInventory = 0;
                    }else{
                        newGiacenzaInventory = article.getInteger("Giacenza") -Integer.parseInt(order.getString("QuantitaDaProdurre"));
                    }
                    order.append("QuantitaDaProdurre",String.valueOf(newQuantitaDaProdurre));

                    Document filter = new Document("OrdineID",Integer.parseInt((String)body.get("OrdineID")));
                    if(newQuantitaDaProdurre==0){
                        Document update = new Document("$set", new Document()
                                .append("ScaricoEffettuato", true));
                        Document doc = database.getCollection("Orders").findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER));
                        //insert in the list the updated document
                        list.add(doc);
                    }
                    Document update2 = new Document("$set",new Document()
                            .append("Giacenza", newGiacenzaInventory));
                    Document filter2 = new Document("ArticoloID",articleId);
                    Document doc2 = database.getCollection("Inventory").findOneAndUpdate(filter2, update2, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                    if(newQuantitaDaProdurre==0)return list;
                }
                for (Document doc : connection) {
                    int temp= 0;
                    temp += Integer.parseInt(doc.getString("CoefficienteFabbisogno")) * Integer.parseInt(order.getString("QuantitaDaProdurre"));
                    Document update = new Document(new Document()
                            .append("FabbisognoID", lastFabbisognoId+1)
                            .append("ArticoloID", doc.getString("ArticoloID_figlio"))
                            .append("OrdineId", body.get("OrdineID"))
                            .append("QuantitaFabbisogno", temp));
                    collection.insertOne(update);
                    updatedDocuments.add(update);
                    lastFabbisognoId++;
                }
                return updatedDocuments;
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @GetMapping(value= "/getOrderByID/{id}", produces = "application/json")
    public Document getOrderByID(@PathVariable String id) {
        String connectionString = conn;
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("Esercitazioni");
                MongoCollection<Document> collection = database.getCollection("Orders");
                Document doc = collection.find(eq("OrdineID", Integer.parseInt(id))).first();
                FindIterable<Document> FabOrder = database.getCollection("Fabbisogni").find(eq("OrdineId",id));
                List<Document> fabbisogni = new ArrayList<>();
                for (Document fab : FabOrder) {
                    fabbisogni.add(fab);
                }
                assert doc != null;
                doc.append("Fabbisogni", fabbisogni);
                return doc;
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @PostMapping(value = "/confirmOrder", consumes = "application/json", produces = "application/json")
    public Document confirmOrder(@RequestBody Map<String, Object> body) {
        String connectionString = conn;
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("Esercitazioni");
                MongoCollection<Document> collection = database.getCollection("Orders");
                Document filter = new Document("OrdineID", Integer.parseInt((String)body.get("OrdineID")));
                Document update = new Document("$set", new Document()
                        .append("ScaricoEffettuato", true));
                Document doc = collection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                //i need to subtract the Fabbisogni quantities from the Giacenza of the Inventory
                FindIterable<Document> FabOrder = database.getCollection("Fabbisogni").find(eq("OrdineId",body.get("OrdineID")));
                for (Document fab : FabOrder) {
                    Document article = database.getCollection("Inventory").find(eq("ArticoloID", Integer.parseInt((String)fab.getString("ArticoloID")))).first();
                    assert article != null;
                    int newGiacenza = Integer.parseInt(article.getString("Giacenza")) - fab.getInteger("QuantitaFabbisogno");
                    Document filter2 = new Document("ArticoloID",Integer.parseInt(fab.getString("ArticoloID")));
                    Document update2 = new Document("$set", new Document()
                            .append("Giacenza", newGiacenza));
                    Document doc2 = database.getCollection("Inventory").findOneAndUpdate(filter2, update2, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                }
                return doc;
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
