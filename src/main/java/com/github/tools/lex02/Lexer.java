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
import java.util.concurrent.ConcurrentHashMap;

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
    private TokenKin tokenKin;
    private SymbolTable symbolTable;
    private final byte EOI = 0x1A;
    private static Logger log = LoggerFactory.getLogger(Lexer.class);

    private Lexer(String str) {
        Objects.requireNonNull(str);
        symbolTable = new SymbolTable();
        codes = str.toCharArray();
        codes = Arrays.copyOf(codes, codes.length + 1);
        codes[codes.length - 1] = EOI;
    }

    private Lexer init() {
        state = State.INITIALIZE;
        return this;
    }

    private List<Token> parse() {
        List<Token> result = new ArrayList<>();
        do {
            nextChar();
            switch (state) {
                case INITIALIZE:
                    nextState(result);
                    break;
                case INT_1:
                    if (!isAlpha() && !isDigit()) {
                        nextState(result);
                    }
                    state = ch == 'n' ? State.INT_2 : State.IDENTIFIER;
                    addMorpheme();
                    break;
                case INT_2:
                    if (!isAlpha() && !isDigit()) {
                        nextState(result);
                    }
                    state = ch == 't' ? State.INT_3 : State.IDENTIFIER;
                    addMorpheme();
                    break;
                case INT_3:
                    if (isBlank()) {
                        tokenKin = TokenKin.INT;
                        nextState(result);
                    } else {
                        addMorpheme();
                        state = State.IDENTIFIER;
                    }
                    break;
                case INTLITERAL:
                    if (isDigit()) {
                        addMorpheme();
                    } else {
                        nextState(result);
                    }
                    break;
                case IDENTIFIER:
                    if (isDigit() || isAlpha()) {
                        addMorpheme();
                    } else {
                        nextState(result);
                    }
                    break;
                case EQEQ_1:
                    if (ch == '=') {
                        addMorpheme();
                        tokenKin = TokenKin.EQEQ;
                        state = State.EQEQ_2;
                    } else {
                        nextState(result);
                    }
                    break;
                case EQEQ_2:
                case SEMI:
                case EQ:
                    nextState(result);
            }
        } while (ch != EOI);
        return result;
    }

    private void addMorpheme() {
        sbuf = Objects.isNull(sbuf) ? new char[1] : Arrays.copyOf(sbuf, sbuf.length + 1);
        sbuf[sbuf.length - 1] = ch;
    }

    /**
     * 状态流转
     */
    private void nextState(List<Token> result) {
        if (Objects.nonNull(sbuf)) {
            try {
                result.add(new Token(symbolTable.getAttribute(sbuf), tokenKin));
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
                tokenKin = TokenKin.IDENTIFIER;
                break;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                addMorpheme();
                state = State.INTLITERAL;
                tokenKin = TokenKin.INTLITERAL;
                break;
            case '=':
                addMorpheme();
                state = State.EQEQ_1;//直接进入EQEQ状态,如果最后词素不为'=='，再回滚到EQ状态
                tokenKin = TokenKin.EQ;
                break;
            case ';':
                addMorpheme();
                state = State.SEMI;
                tokenKin = TokenKin.SEMI;
                break;
            default: state = State.INITIALIZE;
            //@formatter:on
        }
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
        ch = codes[index < codes.length ? index++ : index];
    }

    static class Token {
        /**
         * Token对应的相关属性
         */
        private Attribute attribute;
        /**
         * Token类型
         */
        private TokenKin tokenKin;

        private Token(Attribute attribute, TokenKin tokenKin) {
            this.attribute = attribute;
            this.tokenKin = tokenKin;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "attribute=" + attribute +
                    ", tokenKin=" + tokenKin +
                    '}';
        }
    }

    static class SymbolTable {
        private Map<Chars, Attribute> attributes;

        private SymbolTable() {
            attributes = new ConcurrentHashMap<>();
        }

        private Attribute getAttribute(char[] morpheme) {
            var chars = new Chars(morpheme);
            var attribute = attributes.get(chars);
            if (Objects.isNull(attribute)) {
                attribute = new Attribute(morpheme);
                attributes.put(chars, attribute);
            }
            return attribute;
        }
    }

    static class Chars {
        private char[] morpheme;

        private Chars(char[] morpheme) {
            this.morpheme = morpheme;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Chars)) return false;
            Chars chars = (Chars) o;
            return Arrays.equals(morpheme, chars.morpheme);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(morpheme);
        }
    }

    static class Attribute {
        private char[] morpheme;

        private Attribute(char[] morpheme) {
            this.morpheme = morpheme;
        }

        @Override
        public String toString() {
            return String.format("morpheme:'%s'", new String(morpheme));
        }
    }

    enum TokenKin {
        //@formatter:off
        IDENTIFIER, INT("int"), EQ("="), INTLITERAL("0-9"),
        SEMI(";"), LT("<"), GT(">"), EQEQ("==");
        //@formatter:on
        private String name;

        TokenKin() {
        }

        TokenKin(String name) {
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
        new Lexer("itt value = 100;").init().parse().forEach(x -> log.info("{}", x));
    }
}