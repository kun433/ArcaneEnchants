package com.arcaneenchants;

/**
 * 附魔强度分层，决定获取途径。
 *
 * <p>SIMPLE：功能单一，直接出现在附魔台与普通战利品箱。
 * <p>COMPLEX：功能复合，不会出现在附魔台，只能通过村民交易或稀有战利品（默认 0.8%）获得。
 */
public enum Tier {
    SIMPLE,
    COMPLEX
}
