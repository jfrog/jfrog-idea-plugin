package org.owasp.webgoat.container.asciidoc;

import java.util.Map;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class OperatingSystemMacro extends InlineMacroProcessor {

  public OperatingSystemMacro(String macroName) {
    super(macroName);
  }

  public OperatingSystemMacro(String macroName, Map<String, Object> config) {
    super(macroName, config);
  }

  @Override
  public Object process(ContentNode contentNode, String target, Map<String, Object> attributes) {
    var osName = System.getProperty("os.name");

    // see
    // https://discuss.asciidoctor.org/How-to-create-inline-macro-producing-HTML-In-AsciidoctorJ-td8313.html for why quoted is used
    return createPhraseNode(contentNode, "quoted", osName);
  }
}
