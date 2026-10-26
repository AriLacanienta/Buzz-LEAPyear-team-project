import { CommonModule } from '@angular/common';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { DisputeService } from '../../../services/dispute.service';

@Component({
  selector: 'app-dispute-resolution',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dispute-resolution.component.html',
  styleUrls: ['./dispute-resolution.component.scss']
})
export class DisputeResolutionComponent {
  readonly disputeId = inject<number>(DIALOG_DATA);
  readonly resolutionMessage = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(2000)]
  });
  saving = false;
  error = '';
  private readonly dialogRef = inject<DialogRef<boolean>>(DialogRef);
  private readonly disputes = inject(DisputeService);
  private readonly destroyRef = inject(DestroyRef);

  cancel(): void {
    if (!this.saving) {
      this.dialogRef.close();
    }
  }

  resolve(): void {
    this.resolutionMessage.markAsTouched();
    if (this.saving || this.resolutionMessage.invalid) {
      return;
    }
    this.saving = true;
    this.error = '';
    this.dialogRef.disableClose = true;
    this.disputes.resolve(this.disputeId, this.resolutionMessage.value.trim()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.saving = false;
        this.dialogRef.disableClose = false;
      })
    ).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.error = 'Unable to resolve this dispute. Please retry. You must be a manager resolving another account\'s dispute.';
      }
    });
  }
}
