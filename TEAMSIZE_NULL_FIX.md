# TeamSize Null Safety Fix

## Problem
The `teamSize` field could potentially be sent as `null` in API responses, especially when fetching tournaments by ID. This would cause issues in the Flutter frontend which expects a non-null value.

## Solution - Multiple Layers of Protection

### 1. Entity Level (`Tournaments.java`)
- **Added `@PostLoad` hook**: Normalizes `teamSize` when entity is loaded from database
- **Existing `@PrePersist` and `@PreUpdate` hooks**: Already normalize `teamSize` on create/update
- **Database constraint**: Column is marked as `nullable = false`

```java
@PostLoad
protected void onLoad() {
    // 🔥 CRITICAL: Ensure teamSize is never null when loaded from database
    normalizeTeamSize();
}
```

### 2. Service Level (`TournamentService.java`)
- **Enhanced `mapToDTO()` method**: Added null check and default to "SOLO" if null
- **Normalization**: Converts to uppercase for consistency

```java
// 🔥 CRITICAL: Ensure teamSize is never null - default to SOLO if null
String teamSize = t.getTeamSize();
if (teamSize == null || teamSize.trim().isEmpty()) {
    teamSize = "SOLO";
    log.warn("Tournament {} has null/empty teamSize, defaulting to SOLO", t.getId());
} else {
    teamSize = teamSize.trim().toUpperCase();
}
dto.setTeamSize(teamSize);
```

### 3. DTO Level (`TournamentsDTO.java`)
- **Custom getter**: Returns "SOLO" if `teamSize` is null or empty
- **Custom setter**: Automatically defaults to "SOLO" and normalizes to uppercase

```java
@JsonProperty("teamSize")
public String getTeamSize() {
    if (teamSize == null || teamSize.trim().isEmpty()) {
        return "SOLO"; // Default to SOLO if null
    }
    return teamSize;
}

@JsonProperty("teamSize")
public void setTeamSize(String teamSize) {
    if (teamSize == null || teamSize.trim().isEmpty()) {
        this.teamSize = "SOLO"; // Default to SOLO if null
    } else {
        this.teamSize = teamSize.trim().toUpperCase();
    }
}
```

### 4. Controller Level (`PublicController.java`)
- **Added teamSize to public response**: Ensures it's included and never null

```java
String teamSize = tournament.getTeamSize();
if (teamSize == null || teamSize.trim().isEmpty()) {
    teamSize = "SOLO";
}
```

## Coverage

All tournament endpoints are now protected:
- ✅ `GET /api/tournaments/{id}` - Protected by all layers
- ✅ `GET /api/public/tournaments/{id}` - Protected by all layers + controller check
- ✅ `GET /api/tournaments` - Protected by all layers
- ✅ `GET /api/public/tournaments` - Protected by all layers
- ✅ `GET /api/tournaments/status/{status}` - Protected by all layers
- ✅ All update endpoints - Protected by entity hooks

## Result

`teamSize` will **NEVER** be null in any API response. It will always default to "SOLO" if somehow null data exists in the database or is passed through the system.

