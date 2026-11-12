package com.dating.backend.matching;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dating.backend.entity.UserProfile;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class TraitService {
    public record Group(String section,String key,String label,int max,List<String> options) {}
    public record Rule(String mode,List<String> values) {}
    public record Data(Map<String,List<String>> answers,Map<String,Rule> preferences) {}
    private final ObjectMapper json=new ObjectMapper();
    private final Map<String,Group> catalog=new LinkedHashMap<>();
    private final EntityManager em;
    public TraitService(EntityManager em) {
        this.em=em;
        try(var stream=new ClassPathResource("traitCatalog.json").getInputStream()) {
            for(var g:json.readValue(stream,Group[].class))catalog.put(g.key(),g);
        } catch(Exception e){throw new IllegalStateException("Trait catalog unavailable",e);}
    }
    public Data read(String raw) {
        if(raw==null||raw.isBlank())return new Data(Map.of(),Map.of());
        try {
            if(raw.length()>30000)throw new IllegalArgumentException();
            var d=json.readValue(raw,Data.class);
            if(d==null)throw new IllegalArgumentException();
            return new Data(d.answers()==null?Map.of():d.answers(),d.preferences()==null?Map.of():d.preferences());
        }catch(Exception e){throw invalid();}
    }
    public String normalize(String raw) {
        var d=read(raw);
        for(var entry:d.answers().entrySet())validateValues(entry.getKey(),entry.getValue());
        for(var entry:d.preferences().entrySet()) {
            var rule=entry.getValue();
            if(rule==null||rule.mode()==null||!Set.of("ANY","SIMILAR","SPECIFIC").contains(rule.mode()))throw invalid();
            validateValues(entry.getKey(),rule.values());
            if("SPECIFIC".equals(rule.mode())&&rule.values().isEmpty())throw invalid();
            if(!"SPECIFIC".equals(rule.mode())&&!rule.values().isEmpty())throw invalid();
        }
        try{return json.writeValueAsString(d);}catch(Exception e){throw invalid();}
    }
    private void validateValues(String key,List<String> values) {
        var g=catalog.get(key);
        if(g==null||values==null||values.size()>g.max()||new HashSet<>(values).size()!=values.size()||!("hobbies".equals(key)?values.stream().allMatch(v->v!=null&&!v.isBlank()&&v.length()<=30&&!v.matches(".*[\\p{Cntrl}<>].*")):g.options().containsAll(values)))throw invalid();
    }
    public String wishes(String raw) {
        var d=read(normalize(raw));
        try{return json.writeValueAsString(new Data(Map.of(),d.preferences()));}catch(Exception e){throw invalid();}
    }
    public String approved(Long id) {
        var snapshot=em.find(ApprovedProfile.class,id);
        if(snapshot!=null)return snapshot.getTraitsJson();
        return em.createQuery("select p from UserProfile p where p.userId=:id",UserProfile.class).setParameter("id",id).getResultStream().findFirst().map(UserProfile::getTraitsJson).orElse(null);
    }
    public MatchPreferenceService.Fit assess(String wishJson,String myJson,String theirJson) {
        var rules=read(wishJson).preferences();var mine=read(myJson).answers();var theirs=read(theirJson).answers();
        double score=0;int compared=0,missing=0;
        for(var entry:rules.entrySet()) {
            var rule=entry.getValue();if("ANY".equals(rule.mode()))continue;
            var desired="SIMILAR".equals(rule.mode())?mine.get(entry.getKey()):rule.values();
            var actual=theirs.get(entry.getKey());
            if(desired==null||desired.isEmpty()||actual==null||actual.isEmpty()){missing++;continue;}
            score+=(double)desired.stream().filter(actual::contains).count()/desired.size();compared++;
        }
        return new MatchPreferenceService.Fit(compared==0?null:(int)Math.round(score*100/compared),compared,
            compared==0?"비교할 성향 정보가 아직 부족해요":"성향 "+compared+"개 항목 비교"+(missing>0?" · "+missing+"개 항목은 정보 부족":""));
    }
    public MatchPreferenceService.Fit combine(MatchPreferenceService.Fit base,MatchPreferenceService.Fit traits) {
        int count=base.criteria()+traits.criteria();
        if(count==0)return new MatchPreferenceService.Fit(null,0,"조건이나 성향 정보가 아직 부족해요");
        int percent=(int)Math.round(((base.percent()==null?0:base.percent())*base.criteria()+(traits.percent()==null?0:traits.percent())*traits.criteria())/(double)count);
        return new MatchPreferenceService.Fit(percent,count,"조건·성향 "+count+"개 항목 기준"+(traits.criteria()>0?" · "+traits.comment():""));
    }
    private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"성향 선택을 확인해 주세요. 허용된 키워드와 항목별 선택 개수만 저장할 수 있습니다.");}
}
