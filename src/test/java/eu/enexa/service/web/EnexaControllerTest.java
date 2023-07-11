package eu.enexa.service.web;

import eu.enexa.model.StartContainerModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;


import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.HashMap;


@RunWith(SpringRunner.class)
@WebMvcTest(EnexaController.class)
public class EnexaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void sendJsonDlAsStringToStartContainer() throws Exception {
        HashMap<String,String> parameters = new HashMap<>();
        parameters.put("http://dice-research.org/DICE-framework/parameters/algorithm", "http://dice-research.org/DICE-framework/algorithms/ConEx");
        parameters.put("http://dice-research.org/DICE-framework/parameters/dimensions", "25");
        parameters.put("http://dice-research.org/DICE-framework/parameters/knowledgeGraph", "http://example.org/experiment1/data/kg/dump.ttl");

        StartContainerModel startContainerModel = new StartContainerModel("http://example.org/experiment1","http://dice-research.org/DICE-framework/v1.0","",parameters);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(startContainerModel);

        this.mockMvc.perform(post("/start-container").contentType(MediaType.APPLICATION_JSON).content(json)).andDo(print()).andExpect(status().isOk())
            .andExpect(content().string(containsString("Hello, Mock")));

    }

}
