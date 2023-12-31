package nz.ac.canterbury.seng302.portfolio.cucumber.selenium;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import nz.ac.canterbury.seng302.portfolio.model.Project;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;

import static nz.ac.canterbury.seng302.portfolio.DateTestHelper.addToDateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selenium Cucumber step definitions for the project details feature.
 */
public class ProjectDetailsStepDefs {

    /**
     * Webdriver used during tests.
     */
    private WebDriver webDriver;

    /**
     * WebDriverWait object that is used to wait until some criteria is met, for example an element to be visible.
     */
    private WebDriverWait wait;

    /**
     * End date of the last sprint.
     */
    private String lastSprintEndDate;

    /**
     * Sets up for scenario by getting a web driver and WebDriverWait object.
     */
    @Before
    public void setUp() {
        webDriver = SeleniumService.getWebDriver();
        wait = SeleniumService.getWait();
    }

    /**
     * Tears down after running scenario by quitting the web driver (thus closing the browser) and setting the web
     * driver to null.
     */
    @After
    public void tearDown() {
        SeleniumService.tearDownWebDriver();
    }

    @Then("I can view a page with details of the project")
    public void i_can_view_a_page_with_details_of_the_project() {
        assertTrue(webDriver.findElement(By.className("title")).isDisplayed());
        assertTrue(webDriver.findElement(By.className("project-desc")).isDisplayed());
        assertTrue(webDriver.findElement(By.className("sprint-block")).isDisplayed());
    }

    @Then("I can create and add all details for a project")
    public void i_can_create_and_add_all_details_for_a_project() {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMonthsAfterNow = now.plusMonths(3);
        String today = dtf.format(now);
        String threeMonthsTime = dtf.format(threeMonthsAfterNow);

        DateTimeFormatter projectdtf = DateTimeFormatter.ofPattern("dd/MMM/yyyy");
        String projectToday = projectdtf.format(now);

        webDriver.findElement(By.className("edit-project-button")).click();
        webDriver.findElement(By.id("projectName")).clear();
        webDriver.findElement(By.id("projectName")).sendKeys("test project");
        webDriver.findElement(By.id("projectEndDate")).clear();
        webDriver.findElement(By.id("projectStartDate")).sendKeys(today);
        webDriver.findElement(By.id("projectEndDate")).clear();
        webDriver.findElement(By.id("projectEndDate")).sendKeys(threeMonthsTime);
        webDriver.findElement(By.id("projectDescription")).clear();
        webDriver.findElement(By.id("projectDescription")).sendKeys("test project desc");

        webDriver.findElement(By.id("projectName")).click();
        webDriver.findElement(By.id("saveButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("title-name")));
        assertEquals("test project", webDriver.findElement(By.className("title-name")).getText());
        assertEquals("test project desc", webDriver.findElement(By.className("project-desc")).getText());
        assertEquals(projectdtf.format(now) + " - " + projectdtf.format(threeMonthsAfterNow),
                webDriver.findElement(By.id("project-date")).getText());
    }

    @When("I browse to the project page")
    public void iBrowseToTheProjectPage() {
        webDriver.findElement(By.id("projectsHeaderButton")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(., 'Project Description')]")));
    }

    @And("I am on the project page")
    public void iAmOnTheProjectPage() {
        iBrowseToTheProjectPage();
    }


    @When("I open the add sprint modal")
    public void iOpenTheAddSprintModal() {
        webDriver.findElement(By.id("addSprintButton")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sprintModalButton")));
    }

    @And("There are {int} sprints")
    public void thereAreSprints(int numSprints) {
        while (!webDriver.findElements(By.id("deleteSprintButton")).isEmpty()) {
            webDriver.findElement(By.id("deleteSprintButton")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteSprintModalButton")));
            webDriver.findElement(By.id("deleteSprintModalButton")).click();
        }
        for (int i = 0; i < numSprints; i++) {
            iAddASprint();
        }
        List<WebElement> sprintDates = webDriver.findElements(By.className("sprint-date"));
        String dateString = sprintDates.get(sprintDates.size()-1).getText();
        lastSprintEndDate = dateString.substring(dateString.indexOf("-")+2);
    }

    @And("I add a sprint")
    public void iAddASprint() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSprintButton")));
        webDriver.findElement(By.id("addSprintButton")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("sprintModalButton")));
        webDriver.findElement(By.id("sprintModalButton")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("sprintModalButton")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(., 'Project Description')]")));
    }

    @Then("The start date should be {int} day after the end date of the previous sprint")
    public void theStartDateShouldBeOneDayAfterTheEndDateOfThePreviousSprint(int numDays) {
        String sprintStartDate = webDriver.findElement(By.id("sprintStartDate")).getAttribute("value");
        String expectedStartDate = addToDateString(lastSprintEndDate, Calendar.DATE, numDays);
        assertEquals(expectedStartDate, sprintStartDate);
    }

    @And("The end date should be {int} weeks after the start date")
    public void theEndDateShouldBeWeeksAfterTheStartDate(int numWeeks) {
        String sprintStartDate = webDriver.findElement(By.id("sprintStartDate")).getAttribute("value");
        String sprintEndDate = webDriver.findElement(By.id("sprintEndDate")).getAttribute("value");
        String expectedEndDate = addToDateString(sprintStartDate, Calendar.DATE, -1);
        expectedEndDate = addToDateString(expectedEndDate, Calendar.WEEK_OF_MONTH, numWeeks);
        assertEquals(expectedEndDate, sprintEndDate);
    }

    /**
     * Increases the date in the given input element by the given number of days.
     * @param inputElement input element with the date to be increased
     * @param numDays number of days to increase by (negative to decrease)
     */
    private void addToSprintInput(WebElement inputElement, int numDays) {
        String sprintStartDate = inputElement.getAttribute("value");
        String newStartDate = addToDateString(sprintStartDate, Calendar.DATE, numDays);
        inputElement.clear();
        inputElement.sendKeys(newStartDate);
        webDriver.findElement(By.id("sprintName")).click();
    }

    @And("I move the start date back by {int} day")
    public void iMoveTheStartDateBackByDay(int numDays) {
        addToSprintInput(webDriver.findElement(By.id("sprintStartDate")), -numDays);
    }

    @Then("The following error is displayed: {string}")
    public void theFollowingErrorIsDisplayed(String expectedErrorMessage) {
//        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sprintDateError")));
//        String actualErrorMessage = webDriver.findElement(By.id("sprintDateError")).getText();
//        assertTrue(actualErrorMessage.contains(expectedErrorMessage));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[@data-test-id='alertMessage'][contains(., '" + expectedErrorMessage + "')]")));
    }

    @When("I open the edit modal for sprint {int}")
    public void iOpenTheEditModalForSprint(int sprintNum) {
        webDriver.findElements(By.id("editSprintButton")).get(sprintNum-1).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sprintModalButton")));
    }

    @And("I move the end date forward by {int} day")
    public void iMoveTheEndDateForwardByDay(int numDays) {
        addToSprintInput(webDriver.findElement(By.id("sprintEndDate")), numDays);
    }


    @And("I browse to edit project page")
    public void iBrowseToEditProjectPage() {
        webDriver.findElement(By.id("editProjectButton")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(., 'Edit Project')]")));
    }

    @When("I edit the start date to more than a year ago")
    public void iEditTheStartDateToMoreThanAYearAgo() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("saveButton")));
        webDriver.findElement(By.id("projectStartDate")).click();
        for(int i = 0; i < 20; i++) {
            webDriver.findElement(By.xpath("//th[contains(., '«')]")).click();
        }
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//td[contains(., '1')]")));
        webDriver.findElement(By.xpath("//td[contains(., '1')]")).click();
        webDriver.findElement(By.id("projectName")).click();
    }

    @Then("I should not be able to save the edit")
    public void iShouldNotBeAbleToSaveTheEdit() {
        assertFalse(webDriver.findElement(By.id("saveButton")).isEnabled());
    }

    @And("start date error message should be displayed")
    public void startDateErrorMessageShouldBeDisplayed() {
        assertTrue(webDriver.findElement(By.id("projectStartDateError")).isDisplayed());
    }
}
