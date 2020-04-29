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
package com.github.tools.lex01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 词法分析器
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/4/27 6:30 下午
 */
public class Lexer {
    /**
     * 符号表
     */
    private SymbolTable symbolTable;
    /**
     * 所有保留字符号串的总长度,当词素的符号串长度>此值时，意味着这是一个标识符
     */
    private int maxLength;
    /**
     * 源文件内容
     */
    private char[] codes;
    /**
     * 当前字符
     */
    private char ch;
    /**
     * 词素缓存
     */
    private char[] sbuf;
    /**
     * 文件索引
     */
    private int index;
    /**
     * 结束符号
     */
    private final byte EOI = 0x1A;
    /**
     * Attribute.flag对应TokenKin枚举ID
     */
    private Map<Integer, Integer> indexs;
    private static Logger log = LoggerFactory.getLogger(Lexer.class);

    private Lexer(String str) {
        Objects.requireNonNull(str);
        codes = str.toCharArray();
        codes = Arrays.copyOf(codes, codes.length + 1);
        codes[codes.length - 1] = EOI;
        symbolTable = new SymbolTable();
        indexs = new ConcurrentHashMap<>();
    }

    /**
     * 读取下一个Token
     *
     * @return
     */
    private Token nextToken() {
        Token result = null;
        loop:
        do {
            nextChar();
            //@formatter:off
            switch (ch) {
                case ' ': case '\t':
                    break;
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
                    result = scanIdent();
                    break loop;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    result = scanNumber();
                    break loop;
                //@formatter:on
                case ';':
                    try {
                        addMorpheme();
                        var attribute = symbolTable.getAttribute(sbuf);
                        result = new Token(attribute, getTokenKin(attribute.flag));
                    } finally {
                        sbuf = null;
                    }
                    break loop;
                default:
                    var token = scanOperator();
                    if (Objects.nonNull(token)) {
                        result = token;
                    }
                    break loop;
            }
        } while (ch != EOI);//当读入到结束符号时退出
        return result;
    }

    /**
     * 开始词法解析
     *
     * @return
     */
    private void parse() {
        while (true) {
            var token = nextToken();
            if (Objects.isNull(token)) break;
            log.info("{}", token);
        }
    }

    /**
     * 根据flag获取TokenKin
     *
     * @param flag
     * @return
     */
    private TokenKin getTokenKin(int flag) {
        return flag < maxLength ? TokenKin.values()[indexs.get(flag)] : TokenKin.IDENTIFIER;
    }

    /**
     * 读取一个完整的特殊符号Token
     */
    private Token scanOperator() {
        if (ch == EOI) return null;
        while (true) {
            switch (ch) {
                //@formatter:off
                case '!': case '%': case '&': case '*':
                case '?': case '+': case '-': case ':':
                case '<': case '=': case '>': case '^':
                case '|': case '~': case '@':
                    break;
                //@formatter:on
                default:
                    try {
                        var attribute = symbolTable.getAttribute(sbuf);
                        return new Token(attribute, getTokenKin(attribute.flag));
                    } finally {
                        prevChar();
                        sbuf = null;
                    }
            }
            addMorpheme();
            var attribute = symbolTable.getAttribute(sbuf);
            var tokenKin = getTokenKin(attribute.flag);
            if (TokenKin.IDENTIFIER == tokenKin) {
                try {
                    sbuf = Arrays.copyOf(sbuf, sbuf.length - 1);
                    attribute = symbolTable.getAttribute(sbuf);
                    return new Token(attribute, getTokenKin(attribute.flag));
                } finally {
                    prevChar();
                    sbuf = null;
                }
            }
            nextChar();
        }
    }

    /**
     * 读取一个完整的数字字面值Token
     */
    private Token scanNumber() {
        while (true) {
            switch (ch) {
                //@formatter:off
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                default:
                   try{
                       var attribute = symbolTable.getAttribute(sbuf);
                       return new Token(attribute,TokenKin.INTLITERAL);
                   }finally{
                       prevChar();
                       sbuf = null;
                   }
                //@formatter:on
            }
            addMorpheme();
            nextChar();
        }
    }

    /**
     * 读取一个完成的标识符Token
     *
     * @return
     */
    private Token scanIdent() {
        while (true) {
            //@formatter:off
            switch (ch) {
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
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                //@formatter:on
                default:
                    try {
                        return getToken(symbolTable.getAttribute(sbuf));
                    } finally {
                        prevChar();
                        sbuf = null;
                    }
            }
            addMorpheme();
            nextChar();
        }
    }

    /**
     * 组装词素
     */
    private void addMorpheme() {
        sbuf = Objects.isNull(sbuf) ? new char[1] : Arrays.copyOf(sbuf, sbuf.length + 1);
        sbuf[sbuf.length - 1] = ch;
    }

    /**
     * 封装返回Token
     *
     * @param attribute
     * @return
     */
    private Token getToken(Attribute attribute) {
        Objects.requireNonNull(attribute);
        return new Token(attribute, getTokenKin(attribute.flag));
    }

    /**
     * 移动索引，读取下一个符号
     */
    private void nextChar() {
        if (index < codes.length) {
            ch = codes[index++];
        }
    }

    /**
     * 移动索引，读取上一个符号
     */
    private void prevChar() {
        ch = codes[--index];
    }

    /**
     * 相关初始化操作
     *
     * @return
     */
    private Lexer init() {
        TokenKin[] tokenKins = TokenKin.values();
        Stream.of(tokenKins).forEach(tokenKin -> {
            var name = tokenKin.name;
            if (Objects.nonNull(name)) {
                var attribute = symbolTable.getAttribute(name.toCharArray());
                var flag = attribute.flag;
                indexs.put(flag, tokenKin.ordinal());
                maxLength = maxLength < flag ? flag : maxLength;
            }
        });
        log.info("maxLength:{}", maxLength);
        return this;
    }

    /**
     * 符号表
     */
    static class SymbolTable {
        /**
         * 记录所有保留字的符号串长度
         */
        private int length;
        private Map<Chars, Attribute> attributes;

        private SymbolTable() {
            attributes = new ConcurrentHashMap<>();
        }

        /**
         * 根据词素获取出对应的Attribute，如果不存在就先添加
         *
         * @param morpheme
         * @return
         */
        private Attribute getAttribute(char[] morpheme) {
            var chars = new Chars(morpheme);
            var attribute = attributes.get(chars);
            if (Objects.isNull(attribute)) {
                length += morpheme.length;
                attribute = new Attribute(morpheme, length);
                attributes.put(chars, attribute);
            }
            return attribute;
        }
    }

    /**
     * 词法单元,即Token序列
     */
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
                    "attribute:" + attribute +
                    ", tokenKin:" + tokenKin +
                    '}';
        }
    }

    /**
     * 目前仅支持一些二元表达式
     */
    enum TokenKin {
        //@formatter:off
        IDENTIFIER, INT("int"), EQ("="), INTLITERAL("0-9"),
        SEMI(";"), LT("<"), GT(">"), EQEQ("=="),
        LTEQ("<="), GTEQ(">="), PLUS("+"), SUB("-"),
        STAR("*"), SLASH("/"), PLUSEQ("+="), SUBEQ("-="),
        STAREQ("*="), SLASHEQ("/=");
        //@formatter:on
        private String name;

        TokenKin() {
        }

        TokenKin(String name) {
            this.name = name;
        }
    }

    static class Attribute {
        /**
         * 词素
         */
        private char[] morpheme;
        /**
         * 保留字标记
         */
        private int flag;

        private Attribute(char[] morpheme, int flag) {
            this.morpheme = morpheme;
            this.flag = flag;
        }

        @Override
        public String toString() {
            return String.format("morpheme:'%s',flag:%s", new String(morpheme), flag);
        }
    }

    /**
     * 对词素进行封装
     */
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

    public static void main(String[] agrs) {
        new Lexer("int i=100;").init().parse();
    }
}