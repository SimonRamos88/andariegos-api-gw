package andariegos.andariegos_api_gw.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;



@Data
public class RegistationSucceedResponse {
    private Integer id;
    // private Long eventId;
    private Event event;
    private String userId;

    public Integer getId() {
        return id;
    }


    public void setId(Integer id) {
        this.id = id;
    }


    public Event getEvent() {
        return event;
    }


    public void setEvent(Event event) {
        this.event = event;
    }


    public String getUserId() {
        return userId;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Event{
         private String idEvent;
        private String name;
        private String description;
        private String date;
        private String city;
        private String address;
        private Integer availableSpots;
        private BigDecimal price;
        private String image1;
        private String image2;
        private String image3;
        public String getIdEvent() {
            return idEvent;
        }
        public void setIdEvent(String idEvent) {
            this.idEvent = idEvent;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getCity() {
            return city;
        }
        public void setCity(String city) {
            this.city = city;
        }
        public String getAddress() {
            return address;
        }
        public void setAddress(String address) {
            this.address = address;
        }
        public Integer getAvailableSpots() {
            return availableSpots;
        }
        public void setAvailableSpots(Integer availableSpots) {
            this.availableSpots = availableSpots;
        }
        public BigDecimal getPrice() {
            return price;
        }
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        public String getImage1() {
            return image1;
        }
        public void setImage1(String image1) {
            this.image1 = image1;
        }
        public String getImage2() {
            return image2;
        }
        public void setImage2(String image2) {
            this.image2 = image2;
        }
        public String getImage3() {
            return image3;
        }
        public void setImage3(String image3) {
            this.image3 = image3;
        }
        public String getDate() {
            return date;
        }
        public void setDate(String date) {
            this.date = date;
        }


    }


    @Override
    public String toString() {
        return "RegistationSucceedResponse{" +
                "id='" + this.id + '\'' +
                ", userId=" + this.userId +
                '}';
    }
}
