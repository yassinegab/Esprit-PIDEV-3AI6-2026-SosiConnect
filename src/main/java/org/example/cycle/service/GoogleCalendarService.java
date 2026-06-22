package org.example.cycle.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.example.user.service.GoogleAuthService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GoogleCalendarService {

    private static final String CALENDAR_SUMMARY = "SOSI — Suivi de Cycle";

    public void syncCycleEvents(LocalDate nextPeriod, LocalDate ovulation, LocalDate fertileStart, LocalDate fertileEnd) throws Exception {
        Calendar service = org.example.user.service.GoogleAuthService.getCalendarService();

        // 1. Trouver ou créer le calendrier SOSI
        String calendarId = getOrCreateSosiCalendar(service);

        // 2. Supprimer les anciens événements SOSI futurs (pour éviter les doublons)
        clearFutureSosiEvents(service, calendarId);

        // 3. Créer les nouveaux événements
        createEvent(service, calendarId, "🩸 Règles (Prévu)", nextPeriod, nextPeriod.plusDays(5), "#ef4444");
        createEvent(service, calendarId, "✨ Ovulation", ovulation, ovulation.plusDays(1), "#f59e0b");
        createEvent(service, calendarId, "🌿 Fenêtre fertile", fertileStart, fertileEnd.plusDays(1), "#10b981");
    }

    public List<Event> getEventsForMonth(YearMonth yearMonth) throws Exception {
        Calendar service = GoogleAuthService.getCalendarService();
        String calendarId = getOrCreateSosiCalendar(service);

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        DateTime timeMin = new DateTime(Date.from(startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        DateTime timeMax = new DateTime(Date.from(endOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()));

        return service.events().list(calendarId)
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
                .getItems();
    }

    private String getOrCreateSosiCalendar(Calendar service) throws Exception {
        CalendarList calendarList = service.calendarList().list().execute();
        for (CalendarListEntry entry : calendarList.getItems()) {
            if (CALENDAR_SUMMARY.equals(entry.getSummary())) {
                return entry.getId();
            }
        }

        // Créer un nouveau calendrier
        com.google.api.services.calendar.model.Calendar newCal = new com.google.api.services.calendar.model.Calendar();
        newCal.setSummary(CALENDAR_SUMMARY);
        newCal.setTimeZone("UTC");
        
        com.google.api.services.calendar.model.Calendar createdCal = service.calendars().insert(newCal).execute();
        return createdCal.getId();
    }

    private void clearFutureSosiEvents(Calendar service, String calendarId) throws Exception {
        DateTime now = new DateTime(System.currentTimeMillis());
        List<Event> events = service.events().list(calendarId)
                .setTimeMin(now)
                .execute()
                .getItems();

        for (Event event : events) {
            service.events().delete(calendarId, event.getId()).execute();
        }
    }

    private void createEvent(Calendar service, String calendarId, String title, LocalDate start, LocalDate end, String colorHex) throws Exception {
        Event event = new Event()
                .setSummary(title)
                .setDescription("Généré par SosiProject — Suivi de Cycle");

        // Utiliser des événements "toute la journée" (all-day events)
        EventDateTime startEventTime = new EventDateTime()
                .setDate(new DateTime(start.toString())); // format yyyy-MM-dd
        event.setStart(startEventTime);

        EventDateTime endEventTime = new EventDateTime()
                .setDate(new DateTime(end.toString())); // format yyyy-MM-dd
        event.setEnd(endEventTime);

        service.events().insert(calendarId, event).execute();
    }
}
