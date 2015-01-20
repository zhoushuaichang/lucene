package com.shinowit;


import javafx.scene.control.IndexRange;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.PagedBytes;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.*;
import java.util.Date;

/**
 * Created by Administrator on 2015/1/20.
 */
public class LuceneTest {

    public static void createIndex() throws Exception {
        //indexDir is the directory that hosts Lucene's index files
        File indexDir = new File("D://luceneIndex");//索引存储的位置
        //dataDir is the directory that hosts the text files that to be indexed
        File dataDir = new File("D://txt//唐家三少");//文件所在位置
        Analyzer luceneAnalyzer = new IKAnalyzer();
        File[] dataFiles = dataDir.listFiles();//获取文件列表（数组）

        //有文件系统和内存存储方式，这里使用文件系统来存储索引数据
        Directory directory = new NIOFSDirectory(indexDir);
        //生成全文索引的配置文件
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_10_3, luceneAnalyzer);
        //设置生成全文索引的方式为创建或者追加
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        //创建真正生成全文索引的write对象
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
        long startTime = new Date().getTime();
        for (int i = 0; i < dataFiles.length; i++) {
            if (dataFiles[i].isFile() && dataFiles[i].getName().endsWith(".txt")) {
                System.out.println("Indexing file " + dataFiles[i].getCanonicalPath());
                Document document = new Document();
                Reader txtReader = new FileReader(dataFiles[i]);

                TextField field_name = new TextField("fieldName", dataFiles[i].getPath(), Field.Store.YES);
                document.add(field_name);

                TextField txt_content_field = new TextField("content",getFileReaderContent(dataFiles[i].getPath()),Field.Store.YES);
                document.add(txt_content_field);

                indexWriter.addDocument(document);
            }
        }
        indexWriter.commit();
        indexWriter.close();
        long endTime = new Date().getTime();
        System.out.println("It takes " + (endTime - startTime)
                + " milliseconds to create index for the files in directory "
                + dataDir.getPath());
    }

    public static String getFileReaderContent(String filePath){
        StringBuffer sb=new StringBuffer();
        BufferedReader reader=null;
        try{
            reader=new BufferedReader(new InputStreamReader(new FileInputStream(filePath),"gb2312"));//指定读取文件的编码格式
            String str=null;
            while((str=reader.readLine())!=null){
                sb.append(new String (str.getBytes(),"utf-8"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                reader.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static void query(int pageSize, int pageIndex, String queryString) throws Exception {

        //indexDir is the directory that hosts Lucene's index files
        File indexDir = new File("D://luceneIndex");//索引存储的位置
        Analyzer analyzer = new IKAnalyzer();

        //有文件系统和内存存储方式，这里使用文件系统来存储索引数据
        Directory directory = new NIOFSDirectory(indexDir);

        IndexReader indexReader = DirectoryReader.open(directory);
        //创建搜索类
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        QueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_4_10_3, new String[]{"fieldName", "content"}, analyzer);
        queryParser.setDefaultOperator(QueryParser.OR_OPERATOR);//多个关键字时采取 or 操作

        Query query = queryParser.parse(queryString);

        int start = (pageIndex - 1) * pageSize;
        int max_result_size = start + pageSize;
        TopScoreDocCollector topScoreDoc = TopScoreDocCollector.create(max_result_size, false);
        indexSearcher.search(query, topScoreDoc);

        int rowCount = topScoreDoc.getTotalHits();//满足条件的总记录数
        int pages = (rowCount - 1) / pageSize + 1;//计算总页数

        System.out.println("查到满足条件的记录" + rowCount);
        System.out.println("满足条件的记录页数" + pages);

        TopDocs tds = topScoreDoc.topDocs(start, pageSize);
        ScoreDoc[] scoreDocs = tds.scoreDocs;

        //关键字高亮显示HTML标签，需要导入Lucene-highlight-x.x.x.jar
        SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<font color='red'>", "</font>");
        Highlighter highlighter = new Highlighter(simpleHTMLFormatter, new QueryScorer(query));


        for(int i=0;i<scoreDocs.length;i++){
            //内部编号
            int doc_id=scoreDocs[i].doc;

            //根据文档ID找到文档
            Document mydoc=indexSearcher.doc(doc_id);
            //内容增加高亮显示
            TokenStream tokenStream=analyzer.tokenStream("content",new StringReader(mydoc.get("content")));
            String content=highlighter.getBestFragment(tokenStream,mydoc.get("content"));
            System.out.println("对应的小说文件:" + mydoc.get("fieldName")+" 高亮内容："+content);
        }
    }
    public static void main(String args[]) throws Exception {

        query(10,1,"美女");
//        createIndex();

    }

}
