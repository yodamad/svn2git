/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { StaticMappingUpdateComponent } from 'app/entities/static-mapping/static-mapping-update.component';
import { StaticMappingService } from 'app/entities/static-mapping/static-mapping.service';
import { StaticMapping } from 'app/shared/model/static-mapping.model';

describe('Component Tests', () => {
    describe('StaticMapping Management Update Component', () => {
        let comp: StaticMappingUpdateComponent;
        let fixture: ComponentFixture<StaticMappingUpdateComponent>;
        let service: StaticMappingService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [StaticMappingUpdateComponent]
            })
                .overrideTemplate(StaticMappingUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(StaticMappingUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(StaticMappingService);
        });

        describe('save', () => {
            it(
                'Should call update service on save for existing entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new StaticMapping(123);
                    spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.staticMapping = entity;
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
                    const entity = new StaticMapping();
                    spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.staticMapping = entity;
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
