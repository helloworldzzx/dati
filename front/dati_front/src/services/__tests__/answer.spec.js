import { describe, expect, it } from 'vitest'

import { answerLabel, isAnswerCorrect, shouldAutoSubmitMultiple, splitAnswer } from '../answer'

describe('answer helpers', () => {
  it('splits answers written with common separators', () => {
    expect(splitAnswer('A，B C、D')).toEqual(['A', 'B', 'C', 'D'])
  })

  it('judges answers without caring about option order', () => {
    expect(isAnswerCorrect('A,B,C,D', 'D,C,B,A')).toBe(true)
    expect(isAnswerCorrect('A,B,C,D', 'A,B,E,D')).toBe(false)
  })

  it('only auto submits multiple choice after every selected option is correct', () => {
    expect(shouldAutoSubmitMultiple('A,B,C,D', ['A', 'B'])).toBe(false)
    expect(shouldAutoSubmitMultiple('A,B,C,D', ['A', 'B', 'C', 'E'])).toBe(false)
    expect(shouldAutoSubmitMultiple('A,B,C,D', ['A', 'B', 'C', 'D'])).toBe(true)
  })

  it('formats boolean answer labels', () => {
    expect(answerLabel('TRUE')).toBe('正确')
    expect(answerLabel('FALSE')).toBe('错误')
    expect(answerLabel('A,B')).toBe('A,B')
  })
})
