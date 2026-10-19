export type FundsAction = 'ADD' | 'WITHDRAW';

export interface FundsRequest {
  amount: number;
  action: FundsAction;
}
