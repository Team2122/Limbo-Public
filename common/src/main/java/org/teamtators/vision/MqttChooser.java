package org.teamtators.vision;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class MqttChooser {
    private String name;
    private String[] options;

    private Subject<String> choiceSubject = BehaviorSubject.create();

    public MqttChooser() {
    }

    public MqttChooser(String name, String[] options) {
        this.name = name;
        this.options = options;
    }

    public void updateChoice(String value) {
        choiceSubject.onNext(value);
    }

    @JsonIgnore
    public Observable<String> getObservable() {
        return choiceSubject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }
}
