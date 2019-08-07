package help;
import org.apache.lucene.document.Document;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;

/**
 * Class to represent a pseudo-document for an entity.
 * @author Shubham Chatterjee
 * @version 02/25/2019
 */
public class PseudoDocument {
    private ArrayList<Document> documentList;
    private String entity;
    private ArrayList<String> entityList;

    /**
     * Construcor.
     * @param documentList List of documents in the pseudo-document
     * @param entity The entity for which the pseudo-document is made
     * @param entityList The list of entities in the pseudo-document
     */
    @Contract(pure = true)
    public PseudoDocument(ArrayList<Document> documentList, String entity, ArrayList<String> entityList) {
        this.documentList = documentList;
        this.entity = entity;
        this.entityList = entityList;
    }

    /**
     * Method to get the list of documents in the pseudo-document.
     * @return String
     */
    public ArrayList<Document> getDocumentList() {
        return this.documentList;
    }

    /**
     * Method to get the entity of the pseudo-document.
     * @return String
     */
    public String getEntity() {
        return this.entity;
    }

    /**
     * Method to get the list of entities in the pseudo-document.
     * @return ArrayList
     */
    public ArrayList<String> getEntityList() {
        return this.entityList;
    }

    /**
     * Method to check if the entity passed as parameter is what this pseudo-document is about.
     * @param entity The entity to check
     * @return Boolean True if this document is about entity, false otherwise
     */

    public boolean containsEntity(String entity) {
        return  this.entity.equalsIgnoreCase(entity);
    }

    /**
     * Method to check if a Document is contained in the pseudo-document
     * @param document The document to check
     * @return Boolean true if this document is in the pseudo-document
     */

    public boolean containsDocument(Document document) {
        for (Document d : this.documentList) {
            if (d.getField("id").stringValue().equalsIgnoreCase(document.getField("id").toString())) {
                return true;
            }
        }
        return false;
    }
}

