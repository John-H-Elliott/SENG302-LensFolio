package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.Milestone;
import nz.ac.canterbury.seng302.portfolio.repository.MilestoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import java.util.List;

/***
 * Service class for saving, deleting, updating and retrieving milestone objects to the database.
 */
@Service
public class MilestoneService {
    @Autowired
    private MilestoneRepository repository;

    /**
     * Get list of all milestones
     * @return List of milestones
     */
    public List<Milestone> getAllMilestones() {
        return (List<Milestone>) repository.findAll();
    }

    /**
     * Get list of all milestones ordered
     * @return List of ordered milestones
     */
    public List<Milestone> getAllMilestonesOrdered() {
        return repository.findAllByOrderByMilestoneDate();
    }

    /**
     * Saves the given milestone to the database and returns it.
     * @param milestone milestone to be saved
     * @return saved milestone
     */
    public Milestone addMilestone(Milestone milestone) {
        return repository.save(milestone);
    }

    /**
     * Removes the milestone identified by the given id from the database if it exists.
     * @param id id of the milestone to remove
     */
    public void removeMilestone(Integer id) {
        Optional<Milestone> sOptional = repository.findById(id);

        if (sOptional.isPresent()) {
            Milestone milestone = sOptional.get();
            repository.deleteById(milestone.getId());
        }
    }
}
