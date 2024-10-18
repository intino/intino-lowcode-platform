package io.intino.ls.codeinsight.completion;

import io.intino.tara.Language;
import io.intino.tara.model.Element;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Position;

import java.net.URI;

public record CompletionContext(URI uri, Language language, Position position, Element elementOnPosition, Token tokenOnPosition, ParserRuleContext ruleOnPosition,
								String triggerCharacter) {
}