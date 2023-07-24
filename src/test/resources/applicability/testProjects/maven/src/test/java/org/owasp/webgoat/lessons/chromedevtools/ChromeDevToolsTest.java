package org.owasp.webgoat.lessons.chromedevtools;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.owasp.webgoat.container.plugins.LessonTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Benedikt Stuhrmann
 * @since 13/03/19.
 */
@ExtendWith(SpringExtension.class)
public class ChromeDevToolsTest extends LessonTest {

  @BeforeEach
  public void setup() {
    when(webSession.getCurrentLesson()).thenReturn(new ChromeDevTools());
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void NetworkAssignmentTest_Success() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/ChromeDevTools/network")
                .param("network_num", "123456")
                .param("number", "123456"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonCompleted", Matchers.is(true)));
  }

  @Test
  public void NetworkAssignmentTest_Fail() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/ChromeDevTools/network")
                .param("network_num", "123456")
                .param("number", "654321"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonCompleted", Matchers.is(false)));
  }
}
