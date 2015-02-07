package com.betterstories.servlets;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.mahout.classifier.BayesFileFormatter;
import org.apache.mahout.classifier.ClassifierResult;




import org.apache.mahout.classifier.bayes.Algorithm;
import org.apache.mahout.classifier.bayes.BayesAlgorithm;
import org.apache.mahout.classifier.bayes.BayesParameters;
import org.apache.mahout.classifier.bayes.ClassifierContext;
import org.apache.mahout.classifier.bayes.Datastore;
import org.apache.mahout.classifier.bayes.InMemoryBayesDatastore;
import org.apache.mahout.classifier.bayes.InvalidDatastoreException;
import org.apache.mahout.classifier.bayes.TrainClassifier;
import org.apache.mahout.common.nlp.NGrams;
import org.apache.mahout.vectorizer.DefaultAnalyzer;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class ClassificationRequestGate
 */
@WebServlet("/classify")
public class ClassificationRequestGate extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private String inputDirBad;
	private String outputDirBad;
	private String inputDirGood;
	private String outputDirGood;
	private String charset;
	private String databaseOutputDir;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClassificationRequestGate() {
        super();
        
        inputDirBad  = "D:\\BetterStories\\BetterStoriesServerAPI\\corpus\\fortrain";
        outputDirBad = "D:\\BetterStories\\BetterStoriesServerAPI\\corpus\\trainedOutDir";
        
        inputDirGood  = "D:\\BetterStories\\BetterStoriesServerAPI\\corpus\\fortrainGood";
        outputDirGood = "D:\\BetterStories\\BetterStoriesServerAPI\\corpus\\trainedOutDirGood";
        
        charset   = "UTF-8";
        databaseOutputDir = "D:\\BetterStories\\BetterStoriesServerAPI\\corpus\\trainedDb";
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		training("bad", inputDirBad, charset, outputDirBad, databaseOutputDir);
		training("good", inputDirGood, charset, outputDirBad, databaseOutputDir);
		
		/**
		   * Here we build the bayes parameters object that permits to define some
		   * information about the way to stock the training data. Mahout use
		   * apache hadoops in background for save the classification data.
		   * See the hadoops documentation to know more about this object.
		   * Just take care to specify the classifierType and the basePath.
		   */
		
		  BayesParameters bayesParameters = buildBayesParam(charset, databaseOutputDir);
		  	/**
			* Start the training !
			*/
		
		TrainClassifier.trainNaiveBayes(new Path(outputDirBad), new Path(databaseOutputDir), bayesParameters);
		response.getWriter().write("Training is DONE!");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer requested = new StringBuffer();
		String line = "";
		while(line != null) {
			line = request.getReader().readLine();
			requested.append(line);
		}

		JSONObject responseRoot = new JSONObject();
		JSONArray arrayResponse = new JSONArray();
		
		JSONObject root = new JSONObject(requested.toString());
		if (root.has("store")) {
			JSONObject storeElem = root.getJSONObject("store");
			String data = storeElem.getString("data");
			String type = storeElem.getString("type");
			System.out.println(root.toString());
			File file = null;
			if (type.equals("bad")) 
			{
				file = new File(inputDirBad + "\\" + data.replace("\"", "").replace("'", ""));
			} 
			else if (type.equals("good")) 
			{
				file = new File(inputDirGood + "\\" + data.replace("\"", "").replace("'", ""));
			}
			else 
				return;
			
			if (file != null)
				FileUtils.writeStringToFile(file, data, "UTF-8");
			
		} 
		else {
			JSONArray elements = root.getJSONArray("classify");
			for (int i = 0; i < elements.length(); ++i) {
				JSONObject elem = elements.getJSONObject(i);
				String index = elem.getString("index");
				String data = elem.getString("data");
				int result = classify(data);
				
				JSONObject classified = new JSONObject();
				classified.put("index", index);
				classified.put("result", result);
				arrayResponse.put(classified);
			}
			
			responseRoot.put("classified", arrayResponse);
			response.getWriter().write(responseRoot.toString());
		}		
	}
	
	private int classify(String input) {
		try {
			String result;
			try {
				ClassifierResult cres = searchLabel(input, charset, databaseOutputDir); 
				result = cres.getLabel();
				System.out.println(result + " " + cres.getScore() + " " + input );
				return result.equals("bad") ? 1 : 0;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (InvalidDatastoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 2;
		
	}
	
	public void training(String label, String fileToClassify, String charset, String outputDir, String databaseOutputDir) throws IOException {
		 /**
		   * Take the document and associate to a label inside a file that	
		   * respects the apache mahout input format:	
		   * [LABEL] _TAB_ [TEXT]
		   * example:
		   * english mahout is a good product.
		   * french mahout est un bon produit.		
		   * Note the analyzer =&gt; This is a lucene analyzer, by default apache mahout provide one. I used this one.		
		   * In few words the analyzor permits to define how the words will be extracted from your file...		
		   */
		
		  BayesFileFormatter.format(label, new DefaultAnalyzer(), new File(fileToClassify), Charset.forName(charset), new File(outputDir));
		
		  
	}
	
	 private BayesParameters buildBayesParam(String charset, String databaseOutputDir) {
		 
		   BayesParameters bayesParameters = new BayesParameters();	 
		   bayesParameters.setGramSize(2);	 
		   bayesParameters.set("verbose", "true"); //If you want to see what happen.	 
		   bayesParameters.set("classifierType", "bayes");	 
		   bayesParameters.set("defaultCat", "other"); //The default category to return if a label is not found for a specified text.	 
		   bayesParameters.set("encoding", charset);	 
		   bayesParameters.set("alpha_i", "1.0");		 
		   bayesParameters.set("dataSource", "hdfs");
		   bayesParameters.set("basePath", databaseOutputDir);
		   return bayesParameters;
	}

	 /** 
	   * Ask to mahout to find the good label for the specified content.	 
	   *	 
	   * @param contentToClassify	 
	   *            the content to classify.	 
	   * @param charset	 
	   *            the charset of the content.	 
	   * @param databaseOutputDir	 
	   *            mahout database directory.	 
	   * @return label the label retrieved by mahout.	 
	   * @throws InvalidDatastoreException	 
	   * @throws IOException	 
	   */
	  public ClassifierResult searchLabel(String contentToClassify, String charset, String databaseOutputDir) throws InvalidDatastoreException, IOException {	 
	   //define the algorithm to use	 
	   Algorithm algorithm = new BayesAlgorithm();	 
	   //specify the mahout datastore to use. (the path of hadoops database).	 
	   Datastore datastore = new InMemoryBayesDatastore(buildBayesParam(charset, databaseOutputDir));	 
	   //initialize the mahout context.	 
	   ClassifierContext context = new ClassifierContext(algorithm, datastore);	 
	   context.initialize();
	
	   List< String > document = new NGrams( contentToClassify, 2 ).generateNGramsWithoutLabel();
	   
	   //Make the search	 
	   ClassifierResult classifyResult = context.classifyDocument(document.toArray( new String[ document.size() ] ), "good");
	   return classifyResult;	 
	 }

	

}
