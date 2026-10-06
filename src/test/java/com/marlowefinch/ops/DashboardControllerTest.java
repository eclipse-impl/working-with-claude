package com.marlowefinch.ops;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The JSON API through MockMvc, including the 400 error path. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthReportsUpAndTheFixedToday() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.today").value("2026-09-21"));
    }

    @Test
    void kpisDefaultToTheLast30DaysEndingToday() throws Exception {
        mvc.perform(get("/api/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-08-22"))
                .andExpect(jsonPath("$.to").value("2026-09-21"))
                .andExpect(jsonPath("$.onTimeRate").value(0.937))
                .andExpect(jsonPath("$.openTickets").value(114))
                .andExpect(jsonPath("$.revenue").value(360095.5))
                .andExpect(jsonPath("$.orders").value(624));
    }

    @Test
    void kpisAcceptAnExplicitRange() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-07-01"))
                .andExpect(jsonPath("$.to").value("2026-07-31"))
                .andExpect(jsonPath("$.orders").value(679))
                .andExpect(jsonPath("$.revenue").value(480209.5));
    }

    @Test
    void onTimeReturnsOneRowPerCarrier() throws Exception {
        mvc.perform(get("/api/deliveries/on-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].carrier").value("Kessler Logistics"))
                .andExpect(jsonPath("$[1].delivered").value(265))
                .andExpect(jsonPath("$[1].onTime").value(238))
                .andExpect(jsonPath("$[1].rate").value(0.8981));
    }

    @Test
    void lateReturnsOrderCarrierDatesAndDaysLateAndRespectsTheLimit() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-14").param("to", "2026-09-21").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].orderRef").isString())
                .andExpect(jsonPath("$[0].carrier").isString())
                .andExpect(jsonPath("$[0].promisedDate").isString())
                .andExpect(jsonPath("$[0].deliveredDate").isString())
                .andExpect(jsonPath("$[0].daysLate").isNumber());
    }

    @Test
    void lateWithFromAfterToIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-21").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
    }

    @Test
    void ticketsByCategoryReturnsOpenAndTotalPerCategory() throws Exception {
        mvc.perform(get("/api/tickets/by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].category").value("Delivery delay"))
                .andExpect(jsonPath("$[0].open").value(41))
                .andExpect(jsonPath("$[0].total").value(90));
    }

    @Test
    void vendorsIncludeDaysUntilContractEndAndTheNoticeWindowFlag() throws Exception {
        mvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].name").value("Volta Parts GmbH"))
                .andExpect(jsonPath("$[0].contractEnd").value("2026-10-15"))
                .andExpect(jsonPath("$[0].noticeDays").value(30))
                .andExpect(jsonPath("$[0].daysUntilContractEnd").value(24))
                .andExpect(jsonPath("$[0].inNoticeWindow").value(true))
                .andExpect(jsonPath("$[7].inNoticeWindow").value(false));
    }

    /**
     * TODO-232 (AC-1, AC-4): a malformed date is rejected with a 400 and an
     * {"errors":[...]} body instead of leaking a DateTimeParseException as a 500.
     * Previously this test documented the 5xx.
     */
    @Test
    void malformedFromIsRejectedWith400AndAnErrorsList() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "next-tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"));
    }

    @Test
    void malformedToIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/kpis").param("to", "2026-13-45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("to must be an ISO date (YYYY-MM-DD)"));
    }

    @Test
    void fromAfterToIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-21").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
    }

    @Test
    void aRangeOf367DaysIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-01-01").param("to", "2027-01-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("the date range may span at most 366 days"));
    }

    @Test
    void aRangeOf366DaysIsAccepted() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-01-01").param("to", "2027-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2027-01-01"));
    }

    @Test
    void aSingleDayRangeIsAccepted() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-21").param("to", "2026-09-21"))
                .andExpect(status().isOk());
    }

    @Test
    void limitZeroIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("limit must be an integer between 1 and 500"));
    }

    @Test
    void negativeLimitIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limit501IsRejectedWith400() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("limit must be an integer between 1 and 500"));
    }

    @Test
    void limitBoundariesOneAndFiveHundredAreAccepted() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-08-22").param("to", "2026-09-21").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/deliveries/late").param("limit", "500"))
                .andExpect(status().isOk());
    }

    @Test
    void aNonNumericLimitIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "lots"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("limit must be an integer between 1 and 500"));
    }

    @Test
    void severalProblemsInOneRequestProduceSeveralErrors() throws Exception {
        mvc.perform(get("/api/deliveries/late")
                        .param("from", "next-tuesday").param("to", "someday").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[*]", containsInAnyOrder(
                        "from must be an ISO date (YYYY-MM-DD)",
                        "to must be an ISO date (YYYY-MM-DD)",
                        "limit must be an integer between 1 and 500")));
    }

    @Test
    void aBadRangeAndABadLimitAreReportedTogether() throws Exception {
        mvc.perform(get("/api/deliveries/late")
                        .param("from", "2026-09-21").param("to", "2026-09-01").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    void aValidRequestWithAllParametersStillWorks() throws Exception {
        mvc.perform(get("/api/deliveries/late")
                        .param("from", "2026-09-14").param("to", "2026-09-21").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void theDateRulesApplyToEveryEndpointThatTakesFromAndTo() throws Exception {
        for (String path : new String[] {
                "/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"}) {
            mvc.perform(get(path).param("from", "next-tuesday"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"));
            mvc.perform(get(path).param("from", "2026-09-21").param("to", "2026-09-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
            mvc.perform(get(path).param("from", "2026-01-01").param("to", "2027-01-02"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("the date range may span at most 366 days"));
            mvc.perform(get(path).param("from", "2026-09-01").param("to", "2026-09-21"))
                    .andExpect(status().isOk());
        }
    }
}
