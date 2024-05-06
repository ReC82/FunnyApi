package MultiToolApi.devops;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class MultiToolApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiToolApiApplication.class, args);
    }

}


@RestController
class MultiToolController {

	@GetMapping("/")
	public String index()
	{
		return "Welcome To MoreLess User Managment";
	}
	
	@GetMapping("/hello")
	public String sayHello()
	{
		return "Hello Les Fous!";
	}
	
    @GetMapping("/invert")
    public String invertPhrase(@RequestParam("phrase") String phrase,
                               @RequestParam(value = "output", defaultValue = "text") String outputFormat) throws IOException {
        if (phrase == null || phrase.isEmpty()) {
            return generateErrorMessage("Please provide a non-empty phrase.", outputFormat);
        }
        String invertedPhrase = new StringBuilder(phrase).reverse().toString();
        return generateResponse("Inverted Phrase: " + invertedPhrase, outputFormat);
    }

    @GetMapping("/f2c")
    public String fahrenheitToCelsius(@RequestParam("fahrenheit") Double fahrenheit,
                                      @RequestParam(value = "output", defaultValue = "text") String outputFormat) throws IOException {
        if (fahrenheit == null) {
            return generateErrorMessage("Please provide a Fahrenheit temperature.", outputFormat);
        }
        double celsius = (fahrenheit - 32) * 5 / 9;
        return generateResponse(String.format("%.2f°F is %.2f°C", fahrenheit, celsius), outputFormat);
    }

    @GetMapping("/c2f")
    public String celsiusToFahrenheit(@RequestParam("celsius") Double celsius,
                                      @RequestParam(value = "output", defaultValue = "text") String outputFormat) throws IOException {
        if (celsius == null) {
            return generateErrorMessage("Please provide a Celsius temperature.", outputFormat);
        }
        double fahrenheit = (celsius * 9 / 5) + 32;
        return generateResponse(String.format("%.2f°C is %.2f°F", celsius, fahrenheit), outputFormat);
    }

    private String loadHtmlTemplate(String title, String message) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/result.html");
        byte[] templateBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String template = new String(templateBytes);
        return template.replace("{{title}}", title).replace("{{message}}", message);
    }

    
	 // Updated generateResponse method to use HTML template
	    private String generateResponse(String message, String outputFormat) throws IOException {
	        switch (outputFormat.toLowerCase()) {
	            case "html":
	                return loadHtmlTemplate("MultiTool API Response", message);
	            case "json":
	                // Generate JSON response
	                return "{\"message\": \"" + message + "\"}";
	            case "xml":
	                // Generate XML response
	                return "<response><message>" + message + "</message></response>";
	            default:
	                // Default to plain text
	                return message;
	        }
	    }

    // Helper method to generate error message based on output format
    private String generateErrorMessage(String errorMessage, String outputFormat) {
        switch (outputFormat.toLowerCase()) {
            case "html":
                return "<div style='background-color: red; color: white; padding: 10px;'>" + errorMessage + "</div>";
            case "json":
                // Generate JSON response
                return "{\"error\": \"" + errorMessage + "\"}";
            case "xml":
                // Generate XML response
                return "<error><message>" + errorMessage + "</message></error>";
            default:
                // Default to plain text
                return errorMessage;
        }
    }
    
    @GetMapping("/random-joke")
    public String getRandomJoke(@RequestParam(value = "output", defaultValue = "text") String outputFormat) throws IOException {
        // Set up the OpenAI API endpoint for generating a joke
        String apiUrl = "https://api.openai.com/v1/chat/completions";

        // Set up the request payload
        // String requestBody = "{\"model\":\"gpt-3.5-turbo\",\"messages\":\"Tell me a joke.\",\"max_tokens\":50}";
        
        String model = "gpt-3.5-turbo";
        String prompt = "[{\"role\": \"user\", \"content\": \"Tell me a joke related to Devops or ICT.\"}]";
        int maxTokens = 50;
        
        String requestBody = "{\"model\": \"" + model + "\", \"messages\": " + prompt + ", \"max_tokens\": " + maxTokens + "}";

     // Set up the HTTP connection
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer sk-proj-2OZr0ysW4SQmARE0GVLxT3BlbkFJkL7PyOToD9avlxTDLUD8"); // Replace YOUR_API_KEY with your actual API key
        conn.setDoOutput(true);

        // Send the request payload
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read the response
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            // Print the response if needed
            System.out.println(response.toString());

            // Extract the joke content from the response
            JSONObject jsonObject = new JSONObject(response.toString());
            JSONArray choices = jsonObject.getJSONArray("choices");
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            String content = message.getString("content").trim();

            // Generate response using generateResponse method
            return generateResponse(content, outputFormat);
        }
    }
}
