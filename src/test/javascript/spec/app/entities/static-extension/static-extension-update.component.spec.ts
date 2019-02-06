/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { StaticExtensionUpdateComponent } from 'app/entities/static-extension/static-extension-update.component';
import { StaticExtensionService } from 'app/entities/static-extension/static-extension.service';
import { StaticExtension } from 'app/shared/model/static-extension.model';

describe('Component Tests', () => {
    describe('StaticExtension Management Update Component', () => {
        let comp: StaticExtensionUpdateComponent;
        let fixture: ComponentFixture<StaticExtensionUpdateComponent>;
        let service: StaticExtensionService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [StaticExtensionUpdateComponent]
            })
                .overrideTemplate(StaticExtensionUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(StaticExtensionUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(StaticExtensionService);
        });

        describe('save', () => {
            it(
                'Should call update service on save for existing entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new StaticExtension(123);
                    spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.staticExtension = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.update).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );

            it(
                'Should call create service on save for new entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new StaticExtension();
                    spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.staticExtension = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.create).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );
        });
    });
});
