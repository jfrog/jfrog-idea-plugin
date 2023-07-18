package org.owasp.webgoat.lessons.clientsidefiltering;

import static org.mockito.Mockito.when;
import static org.owasp.webgoat.lessons.clientsidefiltering.ClientSideFilteringFreeAssignment.SUPER_COUPON_CODE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.plugins.LessonTest;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author nbaars
 * @since 5/2/17.
 */
public class ClientSideFilteringAssignmentTest extends LessonTest {

  @BeforeEach
  public void setup() {
    when(webSession.getCurrentLesson()).thenReturn(new ClientSideFiltering());
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void success() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/clientSideFiltering/getItForFree")
                .param("checkoutCode", SUPER_COUPON_CODE))
        .andExpect(jsonPath("$.lessonCompleted", CoreMatchers.is(true)));
  }

  @Test
  public void wrongCouponCode() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/clientSideFiltering/getItForFree")
                .param("checkoutCode", "test"))
        .andExpect(
            jsonPath("$.feedback", CoreMatchers.is(messages.getMessage("assignment.not.solved"))))
        .andExpect(jsonPath("$.lessonCompleted", CoreMatchers.is(false)));
  }
}
