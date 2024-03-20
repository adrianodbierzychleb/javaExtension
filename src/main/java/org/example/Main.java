package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;


public class Main {
    private static final Logger log = Logger.getLogger("sample_log");

    public static void main(String[] args) throws IOException {


        Logging.setupLogger();

        // Set up environment variables
        String statsHostname = System.getenv("STATS_HOSTNAME");
        String statsPort = System.getenv("STATS_PORT");
        String statsUsername = System.getenv("STATS_USERNAME");
        String statsPassword = System.getenv("STATS_PASSWORD");
        String urlTimeout = System.getenv("URL_TIMEOUT");
        int urlTimeoutPars = 0;
        if(urlTimeout == null) {
            urlTimeoutPars = 10;
        }else{
            urlTimeoutPars = Integer.parseInt(urlTimeout);
        }
        String waitTime = System.getenv("WAIT_TIME");
        int waitTimePars = 0;
        if(waitTime == null) {
            waitTimePars = 1;
        }else{
            waitTimePars = Integer.parseInt(waitTime);
        }



        try {
            // Get and assign configuration
            Map<String, Object> statsConfig = loadAppConfig();

            while (true) {
                Map<String, Object> results = apiStatsConnector(statsHostname, statsPort, statsUsername, statsPassword, urlTimeoutPars, false);

                for (Map<String, Object> metric : (List<Map<String, Object>>) statsConfig.get("metrics")) {
                    String metricName = (String) metric.get("name");
                    String metricAlias = (String) metric.get("alias");
                    String aggregationType = (String) metric.get("aggregationType");
                    String timeRollupType = (String) metric.get("timeRollUpType");

                    try {
                        if (metric.get("old_" + metricName) == null) {
                            metric.put("old_" + metricName, Integer.parseInt(results.get(metricName).toString()));
                        } else {
                            int newMetricValue = Integer.parseInt(results.get(metricName).toString());
                            int prevMetricValue = (int) metric.get("old_" + metricName);
                            int metricValue = newMetricValue - prevMetricValue;
                            metric.put("old_" + metricName, newMetricValue);
                            log.info(metricAlias + ": " + metricValue);
                        }
                    } catch (NumberFormatException e) {
                        log.warning("Failed to parse metric value for " + metricName);
                    }
                }
            }
        } catch (Exception e) {
            log.severe("Error occurred: " + e.getMessage());
        }
    }







    public static Map<String, Object> loadAppConfig() throws ProcessingException {
        try {
            Yaml yaml = new Yaml();
            String pathConfig = "<path to config yaml>";
            String pathScheam = "<path to schema yaml>";
            FileInputStream inputConf = new FileInputStream(pathConfig);
            FileInputStream inputSchema = new FileInputStream(pathScheam);
            //String yamlStr = inputYaml.toString();
            Map<String, Object> config = yaml.load(inputConf);
            Map<String, Object> schemaYaml = yaml.load(inputSchema);
            
            ObjectMapper mapper = new ObjectMapper();
            // Validate the configuration against the schema
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonNode schemaParse = mapper.convertValue(schemaYaml, JsonNode.class);
            JsonSchema schema = factory.getJsonSchema(schemaParse);           
            JsonNode configParse = mapper.convertValue(config, JsonNode.class);
            ProcessingReport report = schema.validate(configParse);
            


            if (report.isSuccess()) {
                System.out.println("Validation success! 👍");
                return config;
            } else {
                System.err.println("Validation failed!");
                for (ProcessingMessage message : report) {
                    System.err.println("Error validating data: " + message);
                }
                throw new RuntimeException("Validation failed!");
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            throw e;
        } //catch (IOException e) {
            //throw new RuntimeException(e);
        //}
    }


    public static Map<String, Object> apiStatsConnector(String hostname, String port, String username, String password, int requestTimeout, boolean verify) throws IOException, ParseException {
        String url = "https://" + hostname + ":" + port + "/web-services/rest/stats/DNSCachingServer";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");
            String basicAuthHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            httpGet.setHeader("Authorization", basicAuthHeader);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return new ObjectMapper().readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                } else {
                    throw new IOException("HTTP request failed with code " + statusCode);
                }
            }
        }



    }
}
