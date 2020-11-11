package ie.tcd.trrip;

import java.io.IOException;

import java.util.Scanner;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.FileWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import java.io.BufferedWriter;
import org.apache.lucene.util.BytesRef;

import org.apache.lucene.document.Document;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.util.ArrayList; // import the ArrayList class
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.DocIdSetIterator;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;

 class DocumentModel { 

    public String title;
    public String content;
    public String author;
    public String biblo;
    public String id;
    

    public DocumentModel(String title, String content, String author, String biblo,String id){
        this.title = title;
        this.content = content;
        this.author = author;
        this.biblo = biblo;
        this.id = id;     
    }

}


 class DataFetcher {

    public ArrayList<DocumentModel> allDocument  ;

    public DataFetcher(String fileName){
        this.allDocument  = new ArrayList<DocumentModel>();

        try  
        {  
            File file=new File(fileName/*"data/cran.txt"*/);    //creates a new file instance  
            FileReader fr=new FileReader(file);   //reads the file  
            BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream  
            String line;
            DocumentModel model;
            String title = new String();
            String content = new String();
            String author = new String();
            String biblo = new String();
            String token = new String(); 
            String id = new String(); 
            while((line=br.readLine()) != null )  {
                if (line.contains(".I ")){
                    token = "I Token";
                    id = line.replace(".I ","");
                }
                else if (line.equals(".T")){
                    token = "T Token";

                }
                else if (line.equals(".W")){
                    token = "C Token";

                }
                else if (line.equals(".A")){
                    token = "A Token";

                }
                else if (line.equals(".B")){
                    token = "B Token";
                }

                    switch(token){
                        case "I Token":
                        if(content == null) {
                        }else{
                            // System.out.println("we are now writing a new doc");
                            this.allDocument.add(new DocumentModel(title,content.replace(".W",""),author,biblo,id));
                            content = new String();
                            author = new String();
                            biblo = new String();
                            title = new String();
                        }
                        break;
                        case "T Token":
                        if(line.equals(".T")){

                        }else{
                            title += " "+line;
                        }
                        break;

                        case "C Token":
                            content += " "+line;
                        break;

                        case "A Token":
                        if(line.equals(".A")){

                        }else{
                            author += " "+line;
                        }
                        break;

                        case "B Token":
                        if(line.equals(".B")){

                        }else{
                            biblo += " "+line;
                        }
                        break;
                        default:
                        System.out.println("There is something wrong with token");

                    }

            }  
            fr.close();    //closes the stream and release the resources  
            System.out.println("Reading file compleated.");  
        }  
        catch(IOException e)  
        {  
            e.printStackTrace();  
        }   

    }



}



public class QueryIndexer
{
    
    // Directory where the search index will be saved
    private static String INDEX_DIRECTORY = "../index";

    private Analyzer analyzer;
    private Directory directory;

    public QueryIndexer() throws IOException
    {
        // Need to use the same analyzer and index directory throughout, so
        // initialize them here
        this.analyzer = new StandardAnalyzer();
        this.directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
    }

    public void insertFileIndex(ArrayList<DocumentModel> list) throws IOException{
        FieldType ft = new FieldType(TextField.TYPE_STORED);
        ft.setTokenized(true); //done as default
        ft.setStoreTermVectors(true);
        ft.setStoreTermVectorPositions(true);
        ft.setStoreTermVectorOffsets(true);
        ft.setStoreTermVectorPayloads(true);

        // create and configure an index writer
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(this.directory, config);

        for (DocumentModel model : list)
        {
            Document doc = new Document();
            doc.add(new StringField("filename", model.title, Field.Store.YES));
            doc.add(new StringField("id", model.id, Field.Store.YES));
            doc.add(new Field("content", model.content, ft));
            if(model.id.contains("1")){
                System.out.println(": this is line : " + model.content);
            }
            iwriter.addDocument(doc);
        }
        System.out.println("we have compleated the writing part.");
        // close the writer
        iwriter.close();
    }

    public void buildIndex(String[] args) throws IOException
    {

        // Create a new field type which will store term vector information
        FieldType ft = new FieldType(TextField.TYPE_STORED);
        ft.setTokenized(true); //done as default
        ft.setStoreTermVectors(true);
        ft.setStoreTermVectorPositions(true);
        ft.setStoreTermVectorOffsets(true);
        ft.setStoreTermVectorPayloads(true);

        // create and configure an index writer
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);  

        // Add all input documents to the index
        for (String arg : args)
        {
            System.out.printf("Indexing \"%s\"\n", arg);
            String content = new String(Files.readAllBytes(Paths.get(arg)));
            Document doc = new Document();
            doc.add(new StringField("filename", arg, Field.Store.YES));
            doc.add(new Field("content", content, ft));
            iwriter.addDocument(doc);
        }
        
        // close the writer
        iwriter.close();
    }

    public void fetchQuerryScore(ArrayList<DocumentModel> list)throws IOException,ParseException{
        DirectoryReader ireader = DirectoryReader.open(this.directory);
        int counter = 0;
        // Use IndexSearcher to retrieve some arbitrary document from the index        
        IndexSearcher isearcher = new IndexSearcher(ireader);
        String finalContent = "";
        for (DocumentModel model : list){
            counter ++;
            System.out.printf("." + counter);

            finalContent = finalContent + this.searchQuerry(model.content.replace("?",""),isearcher,ireader,counter);
        }
                // close everything when we're done
        ireader.close();
        this.writeToFile(finalContent);

  
    }

    public String searchQuerry(String text,IndexSearcher isearcher,DirectoryReader ireader,int counter) throws IOException,ParseException
    {


		Analyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("content", analyzer);

        
        // if the user entered a querystring
        if (text.length() > 0)
        {
            // parse the query with the parser
            Query query = parser.parse(text);

            // Get the set of results
            ScoreDoc[] hits = isearcher.search(query, 30).scoreDocs;
            String finalContent = "";
            // Print the results
            System.out.printf(" c:" + counter);
            for (int i = 0; i < hits.length; i++)
            {
                Document hitDoc = isearcher.doc(hits[i].doc);
                // System.out.println(hitDoc.toString());
                if(counter == 1){
                    // System.out.println(counter + " 0 " + hitDoc.get("id") + " " + (i+1)+ " " + hits[i].score );
                }
                // query-id 0 document-id relevance
                // query-id Q0 document-id rank score STANDARD
                finalContent = finalContent + "\n" + counter + " Q0 " + hitDoc.get("id") + " " + (i+1) + " " + hits[i].score + "STANDARD";
            }
            return finalContent;

        }
        else{
            System.out.printf(" c:" + text);

        }
        return "";
    }


    public void writeToFile(String content) throws IOException{
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter("../index/result"));
            out.write(content.trim());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null){
                    out.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() throws IOException
    {
        directory.close();
    }

    public static void main(String[] args) throws IOException,ParseException
    {
        
        // if (args.length <= 0)
        // {
        //     System.out.println("Expected corpus as input");
        //     System.exit(1);            
        // }

        DataFetcher fetcher = new DataFetcher("data/cran.txt");
        
        QueryIndexer indexer = new QueryIndexer();

        indexer.insertFileIndex(fetcher.allDocument);
        DataFetcher querryFetcher = new DataFetcher("data/cranquerry.txt");

        indexer.fetchQuerryScore(querryFetcher.allDocument);

        // QueryIndexer qi = new QueryIndexer();
        // qi.buildIndex(args);
        // qi.postingsDemo();
        // qi.shutdown();
    }
}
