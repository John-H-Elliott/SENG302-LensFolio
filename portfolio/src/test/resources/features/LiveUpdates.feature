Feature: Live Updates on the Details Page

 Scenario: Getting live update messages when editing events
    Given I am logged in as admin
    And An event exists
    When I browse to the edit edit page for an event
    And I have the details page open on another tab
    And I edit an event
    Then a live notification appears on the details page with correct message "Is being edited"

  Scenario: Getting live update messages when saving edited events
    Given I am logged in as admin
    And An event exists
    When I browse to the edit edit page for an event
    And I have the details page open on another tab
    And I save an event
    Then a live notification appears on the details page with correct message "Has been saved"

  Scenario: Live update messages disappearing when not editing events
    Given I am logged in as admin
    And An event exists
    When I browse to the edit edit page for an event
    And I have the details page open on another tab
    And I edit an event
    And a live notification appears on the details page with correct message "Is being edited"
    And I stop editing an event
    Then a live notification disappears from the details page after 5 seconds