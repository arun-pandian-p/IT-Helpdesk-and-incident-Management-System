package com.servicedesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedesk.dto.AuthDtos.LoginRequest;
import com.servicedesk.dto.AuthDtos.TokenResponse;
import com.servicedesk.dto.TicketDtos.*;
import com.servicedesk.model.TicketCategory;
import com.servicedesk.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    private String employeeToken;
    private String supportToken;
    private String categoryId;

    @BeforeEach
    void setUp() throws Exception {
        employeeToken = authenticate("employee@example.com", "Password123!");
        supportToken = authenticate("support@example.com", "Password123!");

        List<TicketCategory> categories = categoryRepository.findAll();
        categoryId = categories.get(0).getId();
    }

    private String authenticate(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
        return tokenResp.getAccessToken();
    }

    @Test
    @DisplayName("Complete ticket lifecycle: Create -> Assign -> Update Status -> Resolve Validation -> Resolve -> Reopen")
    void testCompleteTicketFlow() throws Exception {
        // 1. Create Ticket as Employee
        TicketCreateRequest createReq = new TicketCreateRequest();
        createReq.setTitle("VPN connection dropped abruptly during video call");
        createReq.setDescription("GlobalProtect client disconnected and threw gateway timeout error.");
        createReq.setCategoryId(categoryId);
        createReq.setImpact("Individual");
        createReq.setUrgency("High");
        createReq.setDeviceType("Laptop");
        createReq.setOsName("Windows 11");

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticket_number", startsWith("INC-")))
                .andExpect(jsonPath("$.title", is(createReq.getTitle())))
                .andExpect(jsonPath("$.status", is("Open")))
                .andExpect(jsonPath("$.priority", is("Medium"))) // Individual + High = Medium
                .andExpect(jsonPath("$.sla_status", is("Within SLA")))
                .andReturn();

        TicketDto createdTicket = objectMapper.readValue(createResult.getResponse().getContentAsString(), TicketDto.class);
        String ticketId = createdTicket.getId();

        // 2. Assign ticket to Support Engineer -> status transitions to In Progress
        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isOk())
                .andReturn();
        String supportUserId = objectMapper.readTree(meResult.getResponse().getContentAsString()).get("id").asText();

        TicketAssignRequest assignReq = new TicketAssignRequest();
        assignReq.setAssignedTo(supportUserId);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("In Progress")))
                .andExpect(jsonPath("$.assigned_to", is(supportUserId)));

        // 3. Update status to Waiting for User
        TicketStatusUpdateRequest statusReq = new TicketStatusUpdateRequest();
        statusReq.setStatus("Waiting for User");
        statusReq.setReason("Requested VPN client logs from employee");

        mockMvc.perform(post("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Waiting for User")));

        // 4. Try to resolve without resolution notes -> must fail with 422
        TicketResolveRequest invalidResolve = new TicketResolveRequest();
        invalidResolve.setResolutionNotes("");

        mockMvc.perform(post("/api/tickets/" + ticketId + "/resolve")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidResolve)))
                .andExpect(status().isUnprocessableEntity());

        // 5. Resolve with valid resolution notes
        TicketResolveRequest validResolve = new TicketResolveRequest();
        validResolve.setResolutionNotes("Reset authentication session and updated MTU size to 1420.");

        mockMvc.perform(post("/api/tickets/" + ticketId + "/resolve")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResolve)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Resolved")))
                .andExpect(jsonPath("$.resolved_at", notNullValue()));

        // 6. Employee reopens resolved ticket -> status transitions back to In Progress
        mockMvc.perform(post("/api/tickets/" + ticketId + "/reopen")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("In Progress")));
    }

    @Test
    @DisplayName("Troubleshooting checklist update should persist state")
    void testChecklistUpdate() throws Exception {
        // Create ticket
        TicketCreateRequest createReq = new TicketCreateRequest();
        createReq.setTitle("Printer paper jam check test");
        createReq.setDescription("Copier sensor reporting jam");
        createReq.setCategoryId(categoryId);

        MvcResult res = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String ticketId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        // Support updates checklist
        TicketChecklistUpdateRequest chk = new TicketChecklistUpdateRequest();
        chk.setChecklistState("{\"step1\": true, \"step2\": false}");

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/checklist")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chk)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklist_state", is("{\"step1\": true, \"step2\": false}")));
    }

    @Test
    @DisplayName("Internal comments must be filtered out for Employee role")
    void testCommentInternalFiltering() throws Exception {
        // Create ticket
        TicketCreateRequest createReq = new TicketCreateRequest();
        createReq.setTitle("Sensitive incident for comment filtering test");
        createReq.setDescription("Testing internal vs public comments");
        createReq.setCategoryId(categoryId);

        MvcResult res = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String ticketId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        // Support posts internal comment
        TicketCommentCreateRequest intComment = new TicketCommentCreateRequest();
        intComment.setComment("Private internal note for engineering eyes only.");
        intComment.setInternal(true);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intComment)))
                .andExpect(status().isCreated());

        // Employee views ticket -> internal comment should NOT be visible
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(0)));

        // Support views ticket -> internal comment SHOULD be visible
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].comment", is("Private internal note for engineering eyes only.")));
    }
}
