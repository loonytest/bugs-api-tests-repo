package org.loonycorn.restassuredtests;

import io.qameta.allure.*;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.loonycorn.restassuredtests.model.BugRequestBody;

import org.testng.ITestResult;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.Reporter;

import java.util.List;

import static org.hamcrest.Matchers.*;

@Epic("Bugs API Tests")
public class BugsApiTests {

    @BeforeSuite
    void setup() {
        RestAssured.baseURI = "https://f682-49-205-141-34.ngrok-free.app/";
        RestAssured.basePath = "bugs";

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("ngrok-skip-browser-warning", "true")
                .build();
    }

    private ResponseSpecification createResponseSpec(BugRequestBody bug) {
        return new ResponseSpecBuilder()
                .expectBody("createdBy", equalTo(bug.getCreatedBy()))
                .expectBody("priority", equalTo(bug.getPriority()))
                .expectBody("severity", equalTo(bug.getSeverity()))
                .expectBody("title", equalToIgnoringCase(bug.getTitle()))
                .expectBody("completed", equalTo(bug.getCompleted()))
                .build();
    }

    private void logRequestDetails(RequestSpecification requestSpec) {
        ITestResult result = Reporter.getCurrentTestResult();
        String methodName = result.getMethod().getMethodName();

        if (requestSpec != null) {
            QueryableRequestSpecification queryableRequestSpecification = SpecificationQuerier.query(requestSpec);

            Allure.step(methodName + " Request Details", step -> {
                Allure.addAttachment("Endpoint", queryableRequestSpecification.getURI());

                if (queryableRequestSpecification.getBody() != null) {
                    Allure.addAttachment("Request Body", queryableRequestSpecification.getBody().toString());
                }
            });
        }
    }

    private void logResponseDetails(Response response) {
        ITestResult result = Reporter.getCurrentTestResult();
        String methodName = result.getMethod().getMethodName();

        if (response != null) {

            Allure.step(methodName + " Response Details", step -> {
                Allure.addAttachment("Status", String.valueOf(response.getStatusCode()));

                if (response.getBody() != null) {
                    Allure.addAttachment("Response Body", response.getBody().asPrettyString());
                }
            });
        }
    }

    @Test
    @Feature("Bug Creation")
    @Description("Test to create Bug One")
    @Step("Verify first bug successfully created using POST")
    @Severity(SeverityLevel.BLOCKER)
    public void testPOSTCreateBugOne() {
        BugRequestBody bug = new BugRequestBody(
                "Joseph Wang", 3, "High",
                "Cannot filter by category", false
        );

        ResponseSpecification responseSpec = createResponseSpec(bug);

        RequestSpecification requestSpec = RestAssured.given().body(bug);

        logRequestDetails(requestSpec);

        Response response = requestSpec.when().post();

        logResponseDetails(response);

        response.then()
                    .statusCode(201)
                    .spec(responseSpec);
    }


    @Test
    @Feature("Bug Creation")
    @Description("Test to create Bug Two")
    @Step("Verify second bug successfully created using POST")
    @Severity(SeverityLevel.BLOCKER)
    public void testPOSTCreateBugTwo() {
        BugRequestBody bug = new BugRequestBody(
                "Norah Jones", 0, "Critical",
                "Home page does not load", false
        );

        ResponseSpecification responseSpec = createResponseSpec(bug);

        RequestSpecification requestSpec = RestAssured.given().body(bug);

        logRequestDetails(requestSpec);

        Response response = requestSpec.when().post();

        response.then()
                    .statusCode(201)
                    .spec(responseSpec);

        logResponseDetails(response);
    }


   @Test(dependsOnMethods = {"testPOSTCreateBugOne", "testPOSTCreateBugTwo"})
   @Feature("Bug retrieval")
   @Description("Test to retrieve bugs")
   @Step("Verify bug retrieval returns a list of 2 bugs")
   @Severity(SeverityLevel.TRIVIAL)
   public void testGETRetrieveBugs() {
        RequestSpecification requestSpec = RestAssured.given();

        logRequestDetails(requestSpec);

        Response response = requestSpec.when().get();

        response.then()
                    .statusCode(200)
                    .body("size()", equalTo(2));

        logResponseDetails(response);
    }


    @Test(dependsOnMethods = "testGETRetrieveBugs")
    @Feature("Bug update")
    @Description("Test to update Bug One")
    @Step("Update bug using PUT and verify")
    @Severity(SeverityLevel.NORMAL)
    public void testPUTUpdateBugOne() {
        BugRequestBody bug = new BugRequestBody(
                "Joseph Wang", 1, "Critical",
                "Homepage hangs", false
        );

        ResponseSpecification responseSpec = createResponseSpec(bug);

        String bugId = RestAssured
                .get()
                .then()
                    .statusCode(200)
                    .extract().path("bugId[0]");

        RequestSpecification requestSpec = RestAssured.given().pathParam("bug_id", bugId).body(bug);

        logRequestDetails(requestSpec);

        Response response = requestSpec.when().put("/{bug_id}");

        response.then()
                    .statusCode(200)
                    .spec(responseSpec);

        logResponseDetails(response);
    }


    @Test(dependsOnMethods = "testGETRetrieveBugs")
    @Feature("Bug update")
    @Description("Test to update Bug Two")
    @Step("Update bug using PATCH and verify")
    @Severity(SeverityLevel.CRITICAL)
    public void testPATCHUpdateBugTwo() {
        BugRequestBody bug = new BugRequestBody(
                null, null, null,
                null, true
        );

        String bugId = RestAssured
                .get()
                .then()
                    .statusCode(200)
                    .extract().path("bugId[1]");

        RequestSpecification requestSpec = RestAssured.given().pathParam("bug_id", bugId).body(bug);

        logRequestDetails(requestSpec);

        Response response = requestSpec.when().patch("/{bug_id}");

        response.then()
                    .statusCode(200)
                    .body("completed", equalTo(bug.getCompleted()));

        logResponseDetails(response);
    }

    @Test(dependsOnMethods = {"testPUTUpdateBugOne", "testPATCHUpdateBugTwo"})
    @Feature("Bug deletion")
    @Description("Test to delete all bugs")
    @Step("Delete all bugs and verify")
    @Severity(SeverityLevel.MINOR)
    public void testDELETEAllBugs() {
        List<String> bugIds = RestAssured
                .get()
                .then()
                    .statusCode(200)
                    .extract().path("bugId");

        for (String bugId : bugIds) {
            RequestSpecification requestSpec = RestAssured.given().pathParam("bug_id", bugId);

            logRequestDetails(requestSpec);

            Response response = requestSpec.when().delete("/{bug_id}");

            response.then()
                        .statusCode(200)
                        .body("bug_id", equalTo(bugId));

            logResponseDetails(response);
        }

        RequestSpecification requestSpec = RestAssured.given();

        logRequestDetails(requestSpec);

        Response response = requestSpec.when().get();

        response.then()
                .statusCode(200)
                .body("size()", equalTo(0));

        logResponseDetails(response);
    }

}