INSERT INTO roadmap_progress (id, state)
VALUES (
    'main-roadmap',
    '{
      "statuses": {},
      "notes": {},
      "metrics": {
        "leetcode": 0,
        "ocpTopics": 0,
        "ddiaChapters": 0,
        "artifacts": 0,
        "mocks": 0,
        "applications": 0
      },
      "selectedWeek": 1,
      "selectedPhase": 0,
      "lastUpdate": null
    }'::jsonb
)
ON CONFLICT (id) DO NOTHING;
