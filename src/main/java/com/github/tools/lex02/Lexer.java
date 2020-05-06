/*
 * Copyright 2019-2119 gao_xianglong@sina.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tools.lex02;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 词法分析器
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/4/28 2:47 下午
 */
public class Lexer {
    private char ch;
    private int index;
    private char[] sbuf;
    private char[] codes;
    private State state;
    private TokenKind tokenKind;
    private final byte EOI = 0x1A;
    private static Logger log = LoggerFactory.getLogger(Lexer.class);

    private Lexer(String str) {
        Objects.requireNonNull(str);
        codes = str.toCharArray();
        codes = Arrays.copyOf(codes, codes.length + 1);
        codes[codes.length - 1] = EOI;
    }

    private Lexer init() {
        state = State.INITIALIZE;
        return this;
    }

    public Token nextToken() {
        Token result = null;
        loop:
        do {
            nextChar();
            switch (state) {
                case INITIALIZE:
                    nextState();
                    break;
                case INT_1:
                    if (!isAlpha() && !isDigit()) {
                        result = nextState();
                        break loop;
                    }
                    state = ch == 'n' ? State.INT_2 : State.IDENTIFIER;
                    addMorpheme();
                    break;
                case INT_2:
                    if (!isAlpha() && !isDigit()) {
                        result = nextState();
                        break loop;
                    }
                    state = ch == 't' ? State.INT_3 : State.IDENTIFIER;
                    addMorpheme();
                    break;
                case INT_3:
                    if (isBlank()) {
                        tokenKind = TokenKind.INT;
                        result = nextState();
                        break loop;
                    } else {
                        addMorpheme();
                        state = State.IDENTIFIER;
                    }
                    break;
                case INTLITERAL:
                    if (isDigit()) {
                        addMorpheme();
                    } else {
                        result = nextState();
                        break loop;
                    }
                    break;
                case IDENTIFIER:
                    if (isDigit() || isAlpha()) {
                        addMorpheme();
                    } else {
                        result = nextState();
                        break loop;
                    }
                    break;
                case EQEQ_1:
                    if (ch == '=') {
                        addMorpheme();
                        tokenKind = TokenKind.EQEQ;
                        state = State.EQEQ_2;
                    } else {
                        result = nextState();
                        break loop;
                    }
                    break;
                case EQEQ_2:
                case SEMI:
                case EQ:
                default:
                    result = nextState();
                    break loop;
            }
        } while (ch != EOI);
        return result;
    }

    private void parse() {
        while (true) {
            var token = nextToken();
            if (Objects.isNull(token)) break;
            log.info("{}", token);
        }
    }

    private void addMorpheme() {
        sbuf = Objects.isNull(sbuf) ? new char[1] : Arrays.copyOf(sbuf, sbuf.length + 1);
        sbuf[sbuf.length - 1] = ch;
    }

    /**
     * 状态流转
     */
    private Token nextState() {
        Token result = null;
        if (Objects.nonNull(sbuf)) {
            try {
                result = new Token(sbuf, tokenKind);
            } finally {
                sbuf = null;
            }
        }
        state = State.INITIALIZE;
        switch (ch) {
            //@formatter:off
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z':
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case '$': case '_':
                addMorpheme();
                switch (ch) {
                    case 'i': state = State.INT_1; break;
                    default : state = State.IDENTIFIER;
                }
                tokenKind = TokenKind.IDENTIFIER;
                break;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                addMorpheme();
                state = State.INTLITERAL;
                tokenKind = TokenKind.INTLITERAL;
                break;
            case '=':
                addMorpheme();
                state = State.EQEQ_1;//直接进入EQEQ状态,如果最后词素不为'=='，再回滚到EQ状态
                tokenKind = TokenKind.EQ;
                break;
            case ';':
                addMorpheme();
                state = State.SEMI;
                tokenKind = TokenKind.SEMI;
                break;
            default: state = State.INITIALIZE;
            //@formatter:on
        }
        return result;
    }

    /**
     * 验证是否是数字字面值
     *
     * @return
     */
    private boolean isDigit() {
        //@formatter:off
        switch (ch){
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
               return true;
            default: return false;
        }
        //@formatter:on
    }

    /**
     * 验证是否是字幕
     *
     * @return
     */
    private boolean isAlpha() {
        //@formatter:off
        switch (ch){
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z':
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case '$': case '_':
                return true;
            default: return false;
        }
        //@formatter:on
    }

    private boolean isBlank() {
        //@formatter:off
        switch (ch){
            case ' ': case '\t': case '\n':
                return true;
            default: return false;
        }
        //@formatter:on
    }

    private void nextChar() {
        if (index < codes.length) {
            ch = codes[index++];
        }
    }

    static class Token {
        /**
         * Token对应的相关属性
         */
        private char[] morpheme;
        /**
         * Token类型
         */
        private TokenKind tokenKind;

        private Token(char[] morpheme, TokenKind tokenKind) {
            this.morpheme = morpheme;
            this.tokenKind = tokenKind;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "morpheme=" + new String(morpheme) +
                    ", tokenKind=" + tokenKind +
                    '}';
        }
    }

    enum TokenKind {
        //@formatter:off
        IDENTIFIER, INT("int"), EQ("="), INTLITERAL("0-9"),
        SEMI(";"), LT("<"), GT(">"), EQEQ("==");
        //@formatter:on
        private String name;

        TokenKind() {
        }

        TokenKind(String name) {
            this.name = name;
        }
    }

    /**
     * 保留字的各种状态流转
     */
    enum State {
        INITIALIZE, INT_1, INT_2, INT_3, EQ, INTLITERAL, SEMI, EQEQ_1, EQEQ_2, IDENTIFIER;
    }

    public static void main(String[] agrs) {
        new Lexer("int a=100;").init().parse();
    }
}