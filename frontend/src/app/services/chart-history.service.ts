import { Injectable } from '@angular/core';

export type ChartSample = [timestamp: number, value: number];

/** API observation history, not historical market data or portfolio reconstruction. */
@Injectable({ providedIn: 'root' })
export class ChartHistoryService {
  private readonly maxAgeMs = 60 * 60 * 1000;
  private readonly maxSamples = 720;

  read(accountId: number, metric: string, now = Date.now()): ChartSample[] {
    try {
      return this.validate(JSON.parse(localStorage.getItem(this.key(accountId, metric)) || '[]'), now);
    } catch {
      return [];
    }
  }

  record(accountId: number, metric: string, value: number, timestamp = Date.now()): ChartSample[] {
    const samples = this.read(accountId, metric, timestamp);
    if (!Number.isFinite(value) || value < 0 || !Number.isFinite(timestamp)) return samples;
    const history = this.validate([...samples, [timestamp, value]], timestamp);
    try {
      localStorage.setItem(this.key(accountId, metric), JSON.stringify(history));
    } catch {
      // Storage restrictions/quota must not stop live charts.
    }
    return history;
  }

  private key(accountId: number, metric: string): string {
    return `buzz:chart:v1:account:${accountId}:${encodeURIComponent(metric)}`;
  }

  private validate(value: unknown, now: number): ChartSample[] {
    if (!Array.isArray(value)) return [];
    const samples = new Map<number, number>();
    value.forEach(sample => {
      if (Array.isArray(sample) && sample.length === 2 &&
          typeof sample[0] === 'number' && typeof sample[1] === 'number' &&
          Number.isFinite(sample[0]) && Number.isFinite(sample[1]) && sample[1] >= 0 &&
          sample[0] >= now - this.maxAgeMs && sample[0] <= now) {
        samples.set(sample[0], sample[1]);
      }
    });
    return Array.from(samples.entries()).sort((a, b) => a[0] - b[0]).slice(-this.maxSamples);
  }
}