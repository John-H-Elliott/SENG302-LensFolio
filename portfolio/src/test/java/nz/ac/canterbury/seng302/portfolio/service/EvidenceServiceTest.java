package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.Category;
import nz.ac.canterbury.seng302.portfolio.model.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.HighFivers;
import nz.ac.canterbury.seng302.portfolio.model.Tag;
import nz.ac.canterbury.seng302.portfolio.repository.EvidenceRepository;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link EvidenceService} class.
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {
    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private TagService tagService;

    @Mock
    private  CategoryService categoryService;

    @Mock
    private RegisterClientService registerClientService;

    @InjectMocks
    private EvidenceService evidenceService;

    private static final List<Evidence> testEvidences = new ArrayList<>();

    /**
     * setUp list of Evidences for testing which will returned when mocking the repository's method which return list of Evidences.
     */
    @BeforeEach
    void setUp() {
        Evidence evidence1 = new Evidence(0, 1, "testEvidence1", "testEvidence1", new Date(100));
        Evidence evidence2 = new Evidence(0, 1, "testEvidence2", "testEvidence2", new Date(500));
        Evidence evidence3 = new Evidence(0, 1, "testEvidence3", "testEvidence3", new Date(300));
        Evidence evidence4 = new Evidence(0, 1, "testEvidence4", "testEvidence4", new Date(200));
        testEvidences.add(evidence1);
        testEvidences.add(evidence2);
        testEvidences.add(evidence3);
        testEvidences.add(evidence4);
    }

    /**
     * Tests that the getAllEvidences method returns all evidences based on the userId.
     * This test also check that the method return sorted evidence by date. ( evidence with the latest date should be first)
     */
    @Test
    void getEvidences() {
        when(evidenceRepository.findAllByUserId(any(Integer.class))).thenReturn(testEvidences);

        List<Evidence> actualEvidences = evidenceService.getEvidences(1);
        boolean isCorrectlySorted = true;
        for(int i = 1; i < actualEvidences.size(); i++ ) {
            if(actualEvidences.get(i-1).getDate().before(actualEvidences.get(i).getDate())){
                isCorrectlySorted = false;
                break;
            }
        }
        assertTrue(isCorrectlySorted);
        verify(evidenceRepository, times(1)).findAllByUserId(1);
    }

    /**
     * Test that the evidence service can successfully save a new evidence piece to the database.
     */
    @Test
    void testAddEvidence() {
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(testEvidences.get(0));
        assertTrue(evidenceService.addEvidence(testEvidences.get(0)));
    }

    /**
     * Test that the evidence service returns false when an evidence piece is not saved correctly to the database.
     */
    @Test
    void testFailAddEvidence() {
        when(evidenceRepository.save(any(Evidence.class))).thenThrow(new MockitoException("Mockito exception"));
        assertFalse(evidenceService.addEvidence(testEvidences.get(0)));
    }

    /**
     * Tests that the correct user responses are given when fetching the users who have high fived a piece of evidence.
     */
    @Test
    void testGetHighFiversOfEvidence() {
        List<HighFivers> expectedUsers = new ArrayList<>();
        Evidence testEvidence = new Evidence();
        int numUsers = 3;
        for (int i = 0; i < numUsers; i++) {
            String firstName = "First name" + i;
            String lastName = "Last name" + i;
            UserResponse userResponse = UserResponse.newBuilder().setId(i).setFirstName(firstName).setLastName(lastName).build();
            expectedUsers.add(new HighFivers(firstName + " " + lastName, i));
            when(registerClientService.getUserData(i)).thenReturn(userResponse);
            testEvidence.addHighFiverId(i);
        }
        List<HighFivers> actualUsers = evidenceService.getHighFivers(testEvidence);
        for(int i=0; i < actualUsers.size(); i++){
            assertEquals(expectedUsers.get(i).getUserId(), actualUsers.get(i).getUserId());
            assertEquals(expectedUsers.get(i).getName(), actualUsers.get(i).getName());
        }
    }

    /**
     * Tests that no user responses are returned when no users have high fived a piece of evidence.
     */
    @Test
    void testGetHighFiversOfEvidenceWhenNoHighFivers() {
        Evidence testEvidence = new Evidence();
        List<HighFivers> actualUsers = evidenceService.getHighFivers(testEvidence);
        assertEquals(0, actualUsers.size());
    }

    /**
     * Tests that when the method is passed both a valid skill with a valid user attached to the evidence within tag class that
     * the evidence is returned.
     */
    @Test
    void testGetEvidenceWithSkillAndUserWithValidUserAndSkill() {
        int tagId = 1;
        int evidenceId = 1;
        int userId = 1;

        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(evidenceId);
        testEvidence.setUserId(userId);

        Tag validTag = new Tag("Valid_Tag");
        validTag.setTagId(tagId);
        validTag.addEvidence(testEvidence);
        when(tagService.getTag(tagId)).thenReturn(validTag);

        Optional<Evidence> optionalEvidence = Optional.of(testEvidence);
        when(evidenceRepository.findById(evidenceId)).thenReturn(optionalEvidence);

        try {
            List<Evidence> actualEvidences = evidenceService.getEvidencesWithSkillAndUser(tagId, userId);
            ArrayList<Evidence> expectedEvidences = new ArrayList<>();
            expectedEvidences.add(testEvidence);
            assertEquals(expectedEvidences, actualEvidences);
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Tests that when the method is passed both an invalid skill with a valid/invalid user attached to the evidence within tag class that
     * the evidence is not returned. It should also throw a NullPointerException.
     * When the user is valid as it is attached evidence which is stored in the tag it will still produce a NullPointerException.
     */
    @Test
    void testGetEvidenceWithSkillAndUserWithInvalidUserAndSkill() {
        int tagId = 1;
        int userId = 1;

        when(tagService.getTag(tagId)).thenReturn(null); // Invalid Tag

        try {
            evidenceService.getEvidencesWithSkillAndUser(tagId, userId);
            fail(); // It is expected to throw a NullPointerException.
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                fail();
            }
        }
    }

    /**
     * Tests that when the method is passed both a valid skill but the user attached to the evidences
     * are not the same as the ones being searched for that an empty list is returned.
     */
    @Test
    void testGetEvidenceWithSkillAndUserWithInvalidUserAndValidSkill() {
        int tagId = 1;
        int evidenceId = 1;
        int userId = 1;

        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(evidenceId);
        testEvidence.setUserId(2); // Not the same as the userId being searched for.

        Tag validTag = new Tag("Valid_Tag");
        validTag.setTagId(tagId);
        validTag.addEvidence(testEvidence);
        when(tagService.getTag(tagId)).thenReturn(validTag);

        try {
            evidenceService.getEvidencesWithSkillAndUser(evidenceId, userId);
            List<Evidence> actualEvidences = evidenceService.getEvidencesWithSkillAndUser(tagId, userId);
            ArrayList<Evidence> expectedEvidences = new ArrayList<>();
            assertEquals(expectedEvidences, actualEvidences);
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Tests that when searching just for evidences with a certain tag that if the tag is valid all evidences attached are returned.
     */
    @Test
    void testGetEvidenceWithSkillWithValidSkill() {
        int tagId = 1;
        int evidenceId = 1;

        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(evidenceId);

        Tag validTag = new Tag("Valid_Tag");
        validTag.setTagId(tagId);
        validTag.addEvidence(testEvidence);
        when(tagService.getTag(tagId)).thenReturn(validTag);

        try {
            List<Evidence> actualEvidences = evidenceService.getEvidencesWithSkill(tagId);
            ArrayList<Evidence> expectedEvidences = new ArrayList<>();
            expectedEvidences.add(testEvidence);
            assertEquals(expectedEvidences, actualEvidences);
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Tests that when searching just for evidences with a certain tag that if the tag does not exist that a NullPointerException is thrown.
     */
    @Test
    void testGetEvidenceWithSkillWithInvalidSkill() {
        int tagId = 1;

        when(tagService.getTag(tagId)).thenReturn(null);

        try {
            evidenceService.getEvidencesWithSkill(tagId);
            fail(); // It is expected to throw a NullPointerException.
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                fail();
            }
        }
    }

    ///////////////////////////


    /**
     * Tests that when the method is passed both a valid Category with a valid user attached to the evidence within tag class that
     * the evidence is returned.
     */
    @Test
    void testGetEvidenceWithCategoryAndUserWithValidUserAndCategory() {
        int tagId = 1;
        int evidenceId = 1;
        int userId = 1;

        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(evidenceId);
        testEvidence.setUserId(userId);

        Category validTag = new Category("Valid_Tag");
        validTag.setCategoryId(tagId);
        validTag.addEvidence(testEvidence);
        when(categoryService.getCategory(tagId)).thenReturn(validTag);

        Optional<Evidence> optionalEvidence = Optional.of(testEvidence);
        when(evidenceRepository.findById(evidenceId)).thenReturn(optionalEvidence);

        try {
            List<Evidence> actualEvidences = evidenceService.getEvidencesWithCategoryAndUser(tagId, userId);
            ArrayList<Evidence> expectedEvidences = new ArrayList<>();
            expectedEvidences.add(testEvidence);
            assertEquals(expectedEvidences, actualEvidences);
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Tests that when the method is passed both an invalid Category with a valid/invalid user attached to the evidence within tag class that
     * the evidence is not returned. It should also throw a NullPointerException.
     * When the user is valid as it is attached evidence which is stored in the tag it will still produce a NullPointerException.
     */
    @Test
    void testGetEvidenceWithCategoryAndUserWithInvalidUserAndCategory() {
        int tagId = 1;
        int userId = 1;

        when(categoryService.getCategory(tagId)).thenReturn(null); // Invalid Tag

        try {
            evidenceService.getEvidencesWithCategoryAndUser(tagId, userId);
            fail(); // It is expected to throw a NullPointerException.
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                fail();
            }
        }
    }

    /**
     * Tests that when the method is passed both a valid Category but the user attached to the evidences
     * are not the same as the ones being searched for that an empty list is returned.
     */
    @Test
    void testGetEvidenceWithCategoryAndUserWithInvalidUserAndValidCategory() {
        int tagId = 1;
        int evidenceId = 1;
        int userId = 1;

        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(evidenceId);
        testEvidence.setUserId(2); // Not the same as the userId being searched for.

        Category validTag = new Category("Valid_Tag");
        validTag.setCategoryId(tagId);
        validTag.addEvidence(testEvidence);
        when(categoryService.getCategory(tagId)).thenReturn(validTag);

        try {
            evidenceService.getEvidencesWithCategoryAndUser(evidenceId, userId);
            List<Evidence> actualEvidences = evidenceService.getEvidencesWithCategoryAndUser(tagId, userId);
            ArrayList<Evidence> expectedEvidences = new ArrayList<>();
            assertEquals(expectedEvidences, actualEvidences);
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Tests that when searching just for evidences with a certain Category tag that if the tag is valid all evidences attached are returned.
     */
    @Test
    void testGetEvidenceWithCategoryWithValidCategory() {
        int tagId = 1;
        int evidenceId = 1;

        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(evidenceId);

        Category validTag = new Category("Valid_Tag");
        validTag.setCategoryId(tagId);
        validTag.addEvidence(testEvidence);
        when(categoryService.getCategory(tagId)).thenReturn(validTag);

        try {
            List<Evidence> actualEvidences = evidenceService.getEvidencesWithCategory(tagId);
            ArrayList<Evidence> expectedEvidences = new ArrayList<>();
            expectedEvidences.add(testEvidence);
            assertEquals(expectedEvidences, actualEvidences);
        } catch (NullPointerException e) {
            fail();
        }
    }

    /**
     * Tests that when searching just for evidences with a certain tag that if the Category tag does not exist that a NullPointerException is thrown.
     */
    @Test
    void testGetEvidenceWithCategoryWithInvalidCategory() {
        int tagId = 1;

        when(categoryService.getCategory(tagId)).thenReturn(null);

        try {
            evidenceService.getEvidencesWithCategory(tagId);
            fail(); // It is expected to throw a NullPointerException.
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                fail();
            }
        }
    }

    /**
     * Test that the service class returns all evidence without skill tags attached.
     */
    @Test
    void testGetAllEvidenceWithoutSkills() {
        // Sets up the evidence iterable.
        List<Evidence> expectedEvidences = new ArrayList<>();
        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(1);
        expectedEvidences.add(testEvidence);

        when(evidenceRepository.findAll()).thenReturn(expectedEvidences);
        when(evidenceRepository.findById(testEvidence.getEvidenceId())).thenReturn(Optional.of(testEvidence));

        List<Evidence> actualEvidences = evidenceService.getEvidencesWithoutSkills();
        assertEquals(expectedEvidences, actualEvidences);
    }

    /**
     *  Test that the service class returns all evidence without skill tags attached but with a given user ID attached.
     */
    @Test
    void testGetAllEvidenceWithoutSkillsAndWithUser() {
        int userId = 1;
        // Sets up the evidence iterable.
        List<Evidence> expectedEvidences = new ArrayList<>();
        Evidence testEvidence = new Evidence();
        testEvidence.setEvidenceId(1);
        testEvidence.setUserId(userId);
        expectedEvidences.add(testEvidence);

        when(evidenceRepository.findAll()).thenReturn(expectedEvidences);
        when(evidenceRepository.findById(testEvidence.getEvidenceId())).thenReturn(Optional.of(testEvidence));

        List<Evidence> actualEvidences = evidenceService.getEvidencesWithUserAndWithoutSkills(userId);
        assertEquals(expectedEvidences, actualEvidences);
    }



}
