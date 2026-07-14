import { NgTemplateOutlet } from '@angular/common';
import { Component, Input, TemplateRef, signal } from '@angular/core';

/**
 * Windowed card list (ported from the reference VirtualCardList). Renders every item
 * directly when the count is small; above the threshold it renders only the visible
 * slice inside a bounded, scrollable box (.virtual-scroll) so large boards stay usable.
 *
 * Host is display:contents so the small-list path flows into the parent column's flex
 * layout (matching the non-virtualized look).
 */
@Component({
  selector: 'app-virtual-card-list',
  standalone: true,
  imports: [NgTemplateOutlet],
  styles: [':host { display: contents; }'],
  template: `
    @if (items.length <= threshold) {
      @for (item of items; track trackId(item)) {
        <ng-container
          [ngTemplateOutlet]="itemTemplate"
          [ngTemplateOutletContext]="{ $implicit: item }" />
      }
    } @else {
      <div class="virtual-scroll" (scroll)="onScroll($event)">
        <div [style.height.px]="items.length * rowHeight" style="position: relative;">
          <div
            [style.transform]="'translateY(' + startIndex() * rowHeight + 'px)'"
            style="display: flex; flex-direction: column; gap: 8px;">
            @for (item of visibleSlice(); track trackId(item)) {
              <ng-container
                [ngTemplateOutlet]="itemTemplate"
                [ngTemplateOutletContext]="{ $implicit: item }" />
            }
          </div>
        </div>
      </div>
    }
  `,
})
export class VirtualCardListComponent<T> {
  @Input({ required: true }) items: readonly T[] = [];
  @Input({ required: true }) itemTemplate!: TemplateRef<{ $implicit: T }>;
  @Input({ required: true }) trackId!: (item: T) => string;

  readonly threshold = 50;
  readonly rowHeight = 92;
  private readonly overscan = 6;

  private readonly scrollTop = signal(0);
  private readonly viewport = signal(0);

  onScroll(event: Event): void {
    const el = event.target as HTMLDivElement;
    this.scrollTop.set(el.scrollTop);
    this.viewport.set(el.clientHeight);
  }

  startIndex(): number {
    return Math.max(0, Math.floor(this.scrollTop() / this.rowHeight) - this.overscan);
  }

  visibleSlice(): readonly T[] {
    const start = this.startIndex();
    const visibleCount = Math.ceil((this.viewport() || this.rowHeight * 8) / this.rowHeight);
    const end = Math.min(this.items.length, start + visibleCount + this.overscan * 2);
    return this.items.slice(start, end);
  }
}
