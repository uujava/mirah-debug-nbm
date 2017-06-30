package ru.programpark.mirah.typinghooks;

import ru.programpark.mirah.lexer.MirahTokenId;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;

/**
 *
 * @author Markov, markovs@programpark.ru
 * Created on 27.05.2015 12:29:29
 */
public class TokenBalanceFilterFactory {
    
    public static <T extends TokenId> TokenBalance.Filter<T> create(TokenId id) {
        final Tokens token = Tokens.values()[id.ordinal()];
        switch (token) {
            case tIf:
            case tUnless:
            case tWhile:
                /**
                 * Задача - не учитывать в балансе токенов выражения вида
                 * i = 0 if i < 0 # не учитываем
                 * 
                 * if i > 0 # учитываем
                 *   puts i
                 * end unless i == 33 # не учитываем
                 * 
                 * begin
                 *   break
                 * end while true # не учитываем
                 */
                return new TokenBalance.Filter<T>() {
                    @Override
                    public boolean apply(TokenSequence<?> ts) {
                        TokenId prefixToken = null;
                        // Запоминаем, т.к. будем навигироваться по последовательности токенов
                        int offset = ts.offset();
                        // От найденного тега if идем к началу строки bol, пропуская незначащие теги и запоминаем предыдущий токен.
                        while (ts.movePrevious() && ts.token().id() != MirahTokenId.NL) {
                            // Приходится проверяться на сам токен, т.к. в качестве стартовой позиции для поиска может 
                            // прийти как позиция собственно токена, так и позиция конца текущей строки
                            if (MirahTokenId.get(token).equals(ts.token().id()) || 
                                    MirahTokenId.WHITESPACE_AND_COMMENTS.contains(ts.token().id())) {
                                continue;
                            }
                            prefixToken = ts.token().id();
                            break;
                        }
                        // Восстанавливаем. ts.moveNext() необходим после вызова ts.move(offset)
                        ts.move(offset);
                        ts.moveNext();
                        return prefixToken == null || MirahTypingCompletion.ASSIGNMENT_TOKENS.contains(prefixToken);
                    }
                };
            case tClass:
                /** Задача - не учитывать в балансе токенов выражения Object.class, self.class */
                return new TokenBalance.Filter<T>() {
                    @Override
                    public boolean apply(TokenSequence<?> ts) {
                        TokenId prefixToken = null;
                        // Запоминаем, т.к. будем навигироваться по последовательности токенов
                        int offset = ts.offset();
                        while (ts.movePrevious() && ts.token().id() != MirahTokenId.NL) {
                            // Приходится проверяться на сам токен, т.к. в качестве стартовой позиции для поиска может 
                            // прийти как позиция собственно токена, так и позиция конца текущей строки
                            if (MirahTokenId.get(token).equals(ts.token().id())) {
                                continue;
                            }
                            prefixToken = ts.token().id();
                            break;
                        }
                        // Восстанавливаем. ts.moveNext() необходим после вызова ts.move(offset)
                        ts.move(offset);
                        ts.moveNext();
                        return !MirahTokenId.get(Tokens.tDot).equals(prefixToken);
                    }
                };
            default:
                return null;
        }
    }
}
