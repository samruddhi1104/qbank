package com.qbank.qbank;

import java.util.List;

public class FolderInfo {

    private final String name;
    private final String icon;
    private final int total;
    private final int draft;
    private final int submitted;
    private final int approved;
    private final int rejected;

    public FolderInfo(String name, List<Question> questions) {
        this.name = name;
        this.icon = Subjects.icon(name);
        this.total = questions.size();
        this.draft = count(questions, "DRAFT");
        this.submitted = count(questions, "SUBMITTED");
        this.approved = count(questions, "APPROVED");
        this.rejected = count(questions, "REJECTED");
    }

    private static int count(List<Question> list, String status) {
        int n = 0;
        for (Question q : list) {
            if (status.equals(q.getStatus())) {
                n++;
            }
        }
        return n;
    }

    public String getName() { return name; }
    public String getIcon() { return icon; }
    public int getTotal() { return total; }
    public int getDraft() { return draft; }
    public int getSubmitted() { return submitted; }
    public int getApproved() { return approved; }
    public int getRejected() { return rejected; }
}