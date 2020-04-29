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

import java.util.ArrayList;
import java.util.List;

/**
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/4/29 5:24 下午
 */
public class Elevator {
    private State state;
    private int targetStorey;
    private List<Integer> storeys;
    private int storey;
    private Logger log = LoggerFactory.getLogger(Elevator.class);

    private Elevator(State state, int targetStorey) {
        this.state = state;
        this.targetStorey = targetStorey;
        this.storey = state.storey;
        storeys = new ArrayList<>();
    }

    private void start() {
        int temp = storey;
        loop:
        while (true) {
            switch (state) {
                case _1:
                    if (storey < targetStorey) {
                        storey++;
                        state = State._2;
                        storeys.add(storey);
                    } else break loop;
                    break;
                case _2:
                    if (storey < targetStorey) {
                        storey++;
                        state = State._3;
                    } else if (storey > targetStorey) {
                        storey--;
                        state = State._1;
                    } else break loop;
                    storeys.add(storey);
                    break;
                case _3:
                    if (storey < targetStorey) {
                        storey++;
                        state = State._4;
                    } else if (storey > targetStorey) {
                        storey--;
                        state = State._2;
                    } else break loop;
                    storeys.add(storey);
                    break;
                case _4:
                    if (storey < targetStorey) {
                        storey++;
                        state = State._5;
                    } else if (storey > targetStorey) {
                        storey--;
                        state = State._3;
                    } else break loop;
                    storeys.add(storey);
                    break;
                case _5:
                    if (storey > targetStorey) {
                        storey--;
                        state = State._4;
                        storeys.add(storey);
                    } else break loop;
                    break;
            }
        }
        log.info("当前楼层:{}\t目标楼层:{}\t途经:{}楼", temp, targetStorey, storeys);
    }

    enum State {
        _1(1), _2(2), _3(3), _4(4), _5(5);
        public int storey;

        State(int storey) {
            this.storey = storey;
        }
    }

    public static void main(String[] agrs) {
        new Elevator(State._1, 5).start();
        new Elevator(State._5, 3).start();
    }
}