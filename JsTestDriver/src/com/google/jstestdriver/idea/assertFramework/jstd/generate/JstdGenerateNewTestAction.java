package com.google.jstestdriver.idea.assertFramework.jstd.generate;

import com.google.jstestdriver.idea.assertFramework.AbstractJsGenerateAction;
import com.google.jstestdriver.idea.assertFramework.GenerateActionContext;
import com.google.jstestdriver.idea.assertFramework.JsGeneratorUtils;
import com.google.jstestdriver.idea.assertFramework.jstd.JstdTestCaseStructure;
import com.google.jstestdriver.idea.assertFramework.jstd.JstdTestFileStructure;
import com.google.jstestdriver.idea.assertFramework.jstd.JstdTestFileStructureBuilder;
import com.google.jstestdriver.idea.util.JsPsiUtils;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JstdGenerateNewTestAction extends AbstractJsGenerateAction {

  @NotNull
  @Override
  public String getHumanReadableDescription() {
    return "Test Method";
  }

  @Override
  public boolean isEnabled(@NotNull GenerateActionContext context) {
    JstdTestFileStructureBuilder builder = JstdTestFileStructureBuilder.getInstance();
    JstdTestFileStructure fileStructure = builder.buildTestFileStructure(context.getJsFile());
    if (fileStructure.getTestCaseCount() == 0) {
      return false;
    }
    Runnable testGenerator = buildGenerator(fileStructure, context);
    return testGenerator != null;
  }

  @Override
  public void actionPerformed(@NotNull GenerateActionContext context) {
    JstdTestFileStructureBuilder builder = JstdTestFileStructureBuilder.getInstance();
    JstdTestFileStructure fileStructure = builder.buildTestFileStructure(context.getJsFile());
    Runnable testGenerator = buildGenerator(fileStructure, context);
    if (testGenerator != null) {
      testGenerator.run();
    }
  }

  @Nullable
  private static Runnable buildGenerator(@NotNull JstdTestFileStructure fileStructure, @NotNull GenerateActionContext context) {
    int caretOffset = context.getCaretOffsetInDocument();
    JstdTestCaseStructure jstdTestCaseStructure = fileStructure.findEnclosingTestCaseByOffset(caretOffset);
    if (jstdTestCaseStructure != null) {
      JSObjectLiteralExpression testsObjectLiteral = jstdTestCaseStructure.getTestsObjectsLiteral();
      if (testsObjectLiteral != null) {
        return new TestGeneratorOnObjectLiteral(testsObjectLiteral, context);
      } else {
        if (jstdTestCaseStructure.getTestCount() == 0) {
          JSCallExpression callExpression = jstdTestCaseStructure.getEnclosingCallExpression();
          JSArgumentList argumentList = callExpression.getArgumentList();
          JSExpression[] arguments = JsPsiUtils.getArguments(argumentList);
          if (arguments.length == 1 && arguments[0] != null) {
            return new TestGeneratorOnNewlyCreatedObjectLiteral(argumentList, context);
          }
        }
      }
    } else {
      for (JstdTestCaseStructure testCaseStructure : fileStructure.getTestCaseStructures()) {
        JSObjectLiteralExpression testsObjectLiteral = testCaseStructure.getTestsObjectsLiteral();
        if (testsObjectLiteral != null && JsPsiUtils.isStrictlyInside(testsObjectLiteral.getTextRange(), caretOffset)) {
          return new TestGeneratorOnObjectLiteral(testsObjectLiteral, context);
        }
      }
    }
    return null;
  }

  private static class TestGeneratorOnObjectLiteral implements Runnable {

    private final JSObjectLiteralExpression myTestsObjectLiteral;
    private final GenerateActionContext myContext;

    public TestGeneratorOnObjectLiteral(@NotNull JSObjectLiteralExpression testsObjectLiteral, GenerateActionContext context) {
      myTestsObjectLiteral = testsObjectLiteral;
      myContext = context;
    }

    public void run() {
      JsGeneratorUtils.generateProperty(myTestsObjectLiteral, myContext, "test${Name}: function() {|}");
    }
  }

  private static class TestGeneratorOnNewlyCreatedObjectLiteral implements Runnable {

    private final JSArgumentList myArgumentList;
    private final GenerateActionContext myContext;

    public TestGeneratorOnNewlyCreatedObjectLiteral(@NotNull JSArgumentList argumentList,
                                                    @NotNull GenerateActionContext context) {
      myArgumentList = argumentList;
      myContext = context;
    }

    @Override
    public void run() {
      JsGeneratorUtils.generateObjectLiteralWithPropertyAsArgument(myContext, "{\ntest${Name}: function() {|}\n}", myArgumentList, 1);
    }
  }
}
