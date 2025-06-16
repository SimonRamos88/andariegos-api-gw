package andariegos.andariegos_api_gw.dto;

import lombok.Data;

@Data
public class Report {
    private String _id;
    private Long id_event;
    private String id_reporter;
    private String description;
    private String state;
    private String eventName;

       // Getters and Setters
    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public Long getIdEvent() {
        return id_event;
    }

    public void setIdEvent(Long id_event) {
        this.id_event = id_event;
    }

    public String getIdReporter() {
        return id_reporter;
    }

    public void setIdReporter(String idReporter) {
        this.id_reporter = idReporter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
}
