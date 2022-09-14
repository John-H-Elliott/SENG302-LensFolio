package nz.ac.canterbury.seng302.portfolio.controller;

import com.google.protobuf.Timestamp;
import nz.ac.canterbury.seng302.portfolio.model.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.WebLink;
import nz.ac.canterbury.seng302.portfolio.service.EvidenceService;
import nz.ac.canterbury.seng302.portfolio.service.RegisterClientService;
import nz.ac.canterbury.seng302.portfolio.service.UserAccountClientService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.Date;
import java.util.List;

import static nz.ac.canterbury.seng302.portfolio.controller.EvidenceController.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the evidence controller.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = EvidenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc = MockMvcBuilders.standaloneSetup(EvidenceController.class).build();

    @MockBean
    private EvidenceService evidenceService;

    @MockBean
    private UserAccountClientService userAccountClientService; // needed to load application context

    @MockBean
    private RegisterClientService registerClientService; // needed to load application context

    /**
     * Mocked user response which contains the data of the user
     */
    private final UserResponse mockUser = UserResponse.newBuilder()
            .setBio("default bio")
            .setCreated(Timestamp.newBuilder().setSeconds(55))
            .setEmail("hello@test.com")
            .setFirstName("firsttestname")
            .setLastName("lasttestname")
            .setMiddleName("middlettestname")
            .setNickname("niktestname")
            .setPersonalPronouns("He/him")
            .addRoles(UserRole.STUDENT)
            .build();

    /**
     * AuthState object to be used when we mock security context
     */
    public AuthState validAuthState = AuthState.newBuilder()
            .setIsAuthenticated(true)
            .setNameClaimType("name")
            .setRoleClaimType("role")
            .addClaims(ClaimDTO.newBuilder().setType("role").setValue("ADMIN").build()) // Set the mock user's role
            .addClaims(ClaimDTO.newBuilder().setType("nameid").setValue("123456").build()) // Set the mock user's ID
            .setAuthenticationType("AuthenticationTypes.Federation")
            .setName("validtesttoken")
            .build();

    private static final Evidence testEvidence = new Evidence(0 ,0, "test evidence", "test description", new Date());

    @BeforeEach
    public void setup() {
        // Mock the security context
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        when(mockedSecurityContext.getAuthentication()).thenReturn(new PreAuthenticatedAuthenticationToken(validAuthState, ""));
        SecurityContextHolder.setContext(mockedSecurityContext);

        when(userAccountClientService.getUserIDFromAuthState(any(AuthState.class))).thenReturn(1);
        when(registerClientService.getUserData(1)).thenReturn(mockUser);
    }

    /**
     * Tests blue sky scenario for adding evidence.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence200() throws Exception {
        when(evidenceService.addEvidence(any(Evidence.class))).thenReturn(true);
        doCallRealMethod().when(evidenceService).validateEvidence(eq(testEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", testEvidence)).andExpect(status().isOk());
        // TODO Add extra andExpect statements when the returned fragment is finalised

        verify(evidenceService, times(1)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests blue sky scenario for adding evidence with a valid web link.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence200ValidWebLink() throws Exception {
        List<WebLink> validWebLinks = List.of(new WebLink("https://www.google.com"), new WebLink("https://localhost:9000/"),
                new WebLink("https://dbadmin.csse.canterbury.ac.nz/index.php?route=/sql&db=jth141_portfolio-test&table=web_link&pos=0"),
                new WebLink("http://www.site.com:8008"), new WebLink("http://www.example.com/~product?id=1&page=2"));
        when(evidenceService.addEvidence(any(Evidence.class))).thenReturn(true);
        for (WebLink validWebLink : validWebLinks) {
            Evidence evidence = new Evidence(0, 0, "test evidence", "test description", new Date());
            evidence.addWebLink(validWebLink);
            doCallRealMethod().when(evidenceService).validateEvidence(eq(evidence), any(Model.class));

            mockMvc.perform(post("/add-evidence").flashAttr("evidence", evidence)).andExpect(status().isOk());
            // TODO Add extra andExpect statements when the returned fragment is finalised
        }
        verify(evidenceService, times(validWebLinks.size())).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence service failing to add the evidence.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence500() throws Exception {
        when(evidenceService.addEvidence(any(Evidence.class))).thenReturn(false);
        doCallRealMethod().when(evidenceService).validateEvidence(eq(testEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", testEvidence)).andExpect(status().isInternalServerError())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Evidence Not Added. Saving Error Occurred."));

        verify(evidenceService, times(1)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data missing.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400MissingData() throws Exception {
        doCallRealMethod().when(evidenceService).validateEvidence(any(Evidence.class), any(Model.class));

        mockMvc.perform(post("/add-evidence")).andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title is required"))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description is required"))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE, "Correctly formatted date is required"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a title that exceeds 30 characters.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooLongTitle() throws Exception {
        String stringWith31Chars = new String(new char[31]).replace('\0', 't');
        Evidence invalidEvidence = new Evidence(0 ,0, stringWith31Chars, "test description", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title must be less than 30 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a description that exceeds 250 characters.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooLongDescription() throws Exception {
        String stringWith251Chars = new String(new char[251]).replace('\0', 't');
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", stringWith251Chars, new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description must be less than 250 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data not including a date.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400MissingDate() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", "test description", null);
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE, "Correctly formatted date is required"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a title is only one character.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooShortTitle() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "a", "test description", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title must be at least 2 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a description is only one character.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooShortDescription() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", "a", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description must be at least 2 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a title with only punctuation.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400PunctuationTitle() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, ",.!@#$%^&*';:", "test description", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title must contain at least one letter"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a description with only punctuation.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400PunctuationDescription() throws Exception {
        Evidence invalidEvidence = new Evidence(0, 0, "test evidence", ".,!@#$%^&*';:", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description must contain at least one letter"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having an invalid URL.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400InvalidWebLink() throws Exception {
        List<WebLink> invalidWebLinks = List.of(new WebLink(".,!@#$%^&*';:"), new WebLink("www.test.com"),
                new WebLink("something.ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"),
                new WebLink("mke.ccc"));
        for (WebLink invalidWebLink: invalidWebLinks) {
            System.err.println(invalidWebLink.getUrl());
            Evidence invalidEvidence = new Evidence(0, 0, "test evidence", "test description", new Date());
            invalidEvidence.addWebLink(invalidWebLink);
            doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

            mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                    .andExpect(status().isBadRequest())
                    .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                    .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                    .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                    .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE, "Web links must be valid URLs"));
        }
        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having too many web links.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooManyWebLinks() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", "test description", new Date());
        for (int i=0; i<11; i++) {
            invalidEvidence.addWebLink(new WebLink("https://www.google.com"));
        }
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE, "You can only have up to 10 web links"));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }
}
