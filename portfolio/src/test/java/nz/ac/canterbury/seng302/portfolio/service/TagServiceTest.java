package nz.ac.canterbury.seng302.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import nz.ac.canterbury.seng302.portfolio.model.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.Tag;
import nz.ac.canterbury.seng302.portfolio.repository.EvidenceRepository;
import nz.ac.canterbury.seng302.portfolio.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit tests for the {@link TagService} class.
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    EvidenceRepository evidenceRepository;

    @InjectMocks
    private TagService tagService;

    @InjectMocks
    private EvidenceService evidenceService;

    private static final List<Tag> testTags = new ArrayList<>();
    private static final List<Evidence> testEvidences = new ArrayList<>();

    /**
     * setUp list of Tags for testing which will be returned when mocking the repository's method which will return list of Tags.
     */
    @BeforeEach
    void setUp() {
        for (int i = 0; i < 4; i++) {
            Tag tag = new Tag("Test tag " + (i + 1));
            testTags.add(tag);
        }

        /* When evidence has sections for tags, add the tags above to the piece of evidence */
        Evidence evidence1 = new Evidence(0, 1, "testEvidence1", "testEvidence1", new Date(100));
        Evidence evidence2 = new Evidence(0, 1, "testEvidence2", "testEvidence2", new Date(100));
        for (int i = 0; i < testTags.size(); i++) {
            if (i % 2 == 0) {
              evidence1.addTag(testTags.get(i));
            } else {
              evidence2.addTag(testTags.get(i));
            }
        }
        testEvidences.add(evidence1);
        testEvidences.add(evidence2);
    }

    /**
     * Tests that the getAllTags method returns all tags.
     */
    @Test
    void testGetAllTags() {
        when(tagRepository.findAll()).thenReturn(testTags);

        List<Tag> actualTags = tagService.getAllTags();

        assertEquals(actualTags.size(), testTags.size());
        verify(tagRepository, times(1)).findAll();
    }

    /**
     * Tests that the getTagsFromUserId method returns all the tags attached to the user's pieces of evidence.
     */
    @Test
    void testGetTagsFromUserId() {
        int userId = 5;
        when(evidenceRepository.findAllByUserId(userId)).thenReturn(testEvidences);

        List<Tag> actualTags = tagService.getTagsFromUserId(userId);

        assertEquals(new HashSet<>(testTags), new HashSet<>(actualTags));
    }

    /**
     * Tests that the getTagsFromUserId method returns an empty list when the user has no pieces of evidence.
     */
    @Test
    void testGetTagsFromUserIdWhenNoEvidence() {
        int userId = 5;
        when(evidenceRepository.findAllByUserId(userId)).thenReturn(new ArrayList<>());

        List<Tag> actualTags = tagService.getTagsFromUserId(userId);

        assertEquals(new ArrayList<>(), actualTags);
    }


    /**
     * Tests that the removeTag(int tagId) method removes a specific tag.
     */
    @Test
    void testRemoveTag() {
        Tag tag = testTags.get(1);
        int tagId = tag.getTagId();
        doReturn(Optional.of(tag)).when(tagRepository).findById(tagId);
        doNothing().when(tagRepository).deleteById(tagId);
        boolean success = tagService.removeTag(tagId);
        assertTrue(success);
        verify(tagRepository).deleteById(tagId);
    }
}
