package ie.tcd.trrip;

import java.io.IOException;

import java.util.Scanner;

import java.nio.file.Paths;
import java.nio.file.Files;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

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

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.DocIdSetIterator;


 class DocumentModel { 

    public String title;
    public String content;
    public String author;
    public String biblo;
    

    public DocumentModel(String title, String content, String author, String biblo){
        this.title = title;
        this.content = content;
        this.author = author;
        this.biblo = biblo;     
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
            boolean isTokenLine = false;
            while((line=br.readLine()) != null )  {
                if (line.contains(".I ")){
                    // System.out.println("Reading index " +line);
                    isTokenLine = true;
                    token = "I Token";
                    // continue;
                }
                else if (line.equals(".T")){
                    // System.out.println("Reading Title " +line);
                    isTokenLine = true;

                    token = "T Token";
                    // continue;

                }
                else if (line.equals(".W")){
                    // System.out.println("Reading COntent " +line);
                    isTokenLine = true;

                    token = "C Token";
                    // continue;

                }
                else if (line.equals(".A")){
                    // System.out.println("Reading Author " +line);
                    isTokenLine = true;

                    token = "A Token";
                    // continue;

                }
                else if (line.equals(".B")){
                    // System.out.println("Reading Biblo " +line);
                    isTokenLine = true;

                    token = "B Token";
                    // continue;

                }
                if(!isTokenLine){
               
                    switch(token){
                        case "I Token":
                        if(content == null) {
                            
                        }else{
                            this.allDocument.add(new DocumentModel(title,content,author,biblo));
                            content = new String();
                            author = new String();
                            biblo = new String();
                            title = new String();
                        }
                        break;
                        case "T Token":
                        title += " "+line;
                        break;

                        case "C Token":
                        content += " "+line;
                        break;

                        case "A Token":
                        author += " "+line;
                        break;

                        case "B Token":
                        biblo += " "+line;
                        break;
                        default:
                        System.out.println("There is something wrong with token");

                    }
                 // continue;
               }
               else{
                    isTokenLine = false;
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
            doc.add(new Field("content", model.content, ft));
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

    public void fetchQuerryScore(ArrayList<DocumentModel> list)throws IOException{

        System.out.println("we are not working on the querry" + list.length);
        for (DocumentModel model : list){
            this.searchQuerry(model.content);
        }
  
    }

    public void searchQuerry(String text) throws IOException
    {
        DirectoryReader ireader = DirectoryReader.open(this.directory);
    
        // Use IndexSearcher to retrieve some arbitrary document from the index        
        IndexSearcher isearcher = new IndexSearcher(ireader);
        Query queryTerm = new TermQuery(new Term("content",text));
        // System.out.println("this si the text : "+ text);
        ScoreDoc[] hits = isearcher.search(queryTerm, 1).scoreDocs;
        
        // Make sure we actually found something
        if (hits.length <= 0)
        {
            System.out.println("Failed to retrieve a document");
            return;
        }

        // get the document ID of the first search result
        int docID = hits[0].doc;

        // Get the fields associated with the document (filename and content)
        Fields fields = ireader.getTermVectors(docID);

        for (String field : fields)
        {
            // For each field, get the terms it contains i.e. unique words
            Terms terms = fields.terms(field);

            // Iterate over each term in the field
            BytesRef termByte = null;
            TermsEnum termsEnum = terms.iterator();

            while ((termByte = termsEnum.next()) != null)
            {                                
                int id;

                // for each term retrieve its postings list
                PostingsEnum posting = null;
                posting = termsEnum.postings(posting, PostingsEnum.FREQS);

                // This only processes the single document we retrieved earlier
                while ((id = posting.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
                {
                    // convert the term from a byte array to a string
                    String termString = termByte.utf8ToString();
                    
                    // extract some stats from the index
                    Term term = new Term(field, termString);
                    long freq = posting.freq();
                    long docFreq = ireader.docFreq(term);
                    long totFreq = ireader.totalTermFreq(term);

                    // print the results
                    System.out.printf(
                        "%-16s : freq = %4d : totfreq = %4d : docfreq = %4d\n",
                        termString, freq, totFreq, docFreq
                    );
                }
            }
        }

        // close everything when we're done
        ireader.close();
    }

    public void shutdown() throws IOException
    {
        directory.close();
    }

    public static void main(String[] args) throws IOException
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
