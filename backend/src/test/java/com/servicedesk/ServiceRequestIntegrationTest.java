package com.servicedesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedesk.dto.AuthDtos.LoginRequest;
import com.servicedesk.dto.AuthDtos.TokenResponse;
import com.servicedesk.dto.ServiceRequestDtos.*;
import com.servicedesk.model.Constants.ServiceRequestApprovalStatus;
import com.servicedesk.model.Constants.ServiceRequestStatus;
import com.servicedesk.model.ServiceRequestType;
import com.servicedesk.repository.ServiceRequestRepository;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceRequestRepository srRepository;

    private String employeeToken;
    private String adminToken;
    private String supportToken;

    @BeforeEach
    void setUp() throws Exception {
        employeeToken = authenticate("employee@example.com", "Password123!");
        adminToken = authenticate("admin@example.com", "Password123!");
        supportToken = authenticate("support@example.com", "Password123!");
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
    @DisplayName("Should fetch service request catalog types")
    void testGetCatalogTypes() throws Exception {
        mockMvc.perform(get("/api/service-request-types")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(15))))
                .andExpect(jsonPath("$[*].name", hasItems("New Laptop", "Microsoft 365 License", "Password Reset")));
    }

    @Test
    @DisplayName("Complete Service Request flow: Create (Approval Required) -> Admin Approve -> Fulfill -> Confirm")
    void testServiceRequestApprovalAndFulfillment() throws Exception {
        // Find M365 license type
        ServiceRequestType m365Type = srRepository.findTypeByName("Microsoft 365 License")
                .orElseThrow();

        // 1. Employee creates request
        ServiceRequestCreateRequest createReq = new ServiceRequestCreateRequest();
        createReq.setRequestTypeId(m365Type.getId());
        createReq.setTitle("Microsoft 365 Copilot License Request Test");
        createReq.setDescription("Requesting AI license for spreadsheet modeling.");
        createReq.setBusinessJustification("Authorized by department director.");
        createReq.setApplicationName("Microsoft 365 Copilot");

        MvcResult createResult = mockMvc.perform(post("/api/service-requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.request_number", startsWith("SR-")))
                .andExpect(jsonPath("$.approval_required", is(true)))
                .andExpect(jsonPath("$.status", is(ServiceRequestStatus.PENDING_APPROVAL)))
                .andExpect(jsonPath("$.approval_status", is(ServiceRequestApprovalStatus.PENDING)))
                .andReturn();

        String srId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Admin approves request
        ServiceRequestApproveRequest approveReq = new ServiceRequestApproveRequest();
        approveReq.setApproved(true);
        approveReq.setReason("Authorized by director in Q4 budget.");

        mockMvc.perform(post("/api/service-requests/" + srId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(ServiceRequestStatus.APPROVED)))
                .andExpect(jsonPath("$.approval_status", is(ServiceRequestApprovalStatus.APPROVED)));

        // 3. Support fulfills request
        ServiceRequestFulfillRequest fulfillReq = new ServiceRequestFulfillRequest();
        fulfillReq.setResolutionNotes("License provisioned in Microsoft 365 Admin Center.");
        fulfillReq.setFulfillmentDetails(Map.of("assignedSku", "O365_BUSINESS_PREMIUM_COPILOT"));

        mockMvc.perform(post("/api/service-requests/" + srId + "/fulfill")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fulfillReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(ServiceRequestStatus.FULFILLED)))
                .andExpect(jsonPath("$.fulfilled_at", notNullValue()));

        // 4. Employee confirms delivery
        ServiceRequestConfirmRequest confirmReq = new ServiceRequestConfirmRequest();
        confirmReq.setFeedbackNotes("Copilot is working as expected. Thank you!");

        mockMvc.perform(post("/api/service-requests/" + srId + "/confirm")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(ServiceRequestStatus.CLOSED)))
                .andExpect(jsonPath("$.closed_at", notNullValue()));
    }
}
