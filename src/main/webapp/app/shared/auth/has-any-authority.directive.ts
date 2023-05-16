import { Directive, Input, TemplateRef, ViewContainerRef } from '@angular/core';

/**
 * @whatItDoes Conditionally includes an HTML element if current user has any
 * of the authorities passed as the `expression`.
 *
 * @howToUse
 * ```
 *     <some-element *jhiHasAnyAuthority="'ROLE_ADMIN'">...</some-element>
 *
 *     <some-element *jhiHasAnyAuthority="['ROLE_ADMIN', 'ROLE_USER']">...</some-element>
 * ```
 */
@Directive({
    selector: '[jhiHasAnyAuthority]'
})
export class HasAnyAuthorityDirective {
    private authorities: string[];

    constructor(private templateRef: TemplateRef<any>, private viewContainerRef: ViewContainerRef) {}

    @Input()
    set jhiHasAnyAuthority(value: string | string[]) {
        this.authorities = typeof value === 'string' ? [value] : value;
        this.updateView();
    }

    private updateView(): void {}
}
