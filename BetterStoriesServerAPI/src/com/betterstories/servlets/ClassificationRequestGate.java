package com.betterstories.servlets;

import java.io.IOException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClassificationRequestGate() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().write("Method do GET was called!");
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
	
	private int classify(String input) {
		return Math.random() < 0.5 ? 0 : 1;
	}
}
