package com.qbank.qbank;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

@Controller
public class QuestionController {

    private final QuestionRepository repo;

    public QuestionController(QuestionRepository repo) {
        this.repo = repo;
    }

    // ---------- values available to every page ----------
    @ModelAttribute("role")
    public String roleAttr(HttpSession session) {
        Object r = session.getAttribute("role");
        return r == null ? "TEACHER" : r.toString();
    }

    @ModelAttribute("self")
    public String selfAttr(HttpServletRequest req) {
        return "GET".equals(req.getMethod()) ? req.getRequestURI() : "/questions";
    }

    @ModelAttribute("subjects")
    public List<String> subjectsAttr() {
        return Subjects.ALL;
    }

    // ---------- helpers ----------
    private String folderUrl(String subject) {
        return "/folders/" + UriUtils.encodePathSegment(subject, StandardCharsets.UTF_8);
    }

    private String safeBack(String back, String fallback) {
        if (back != null && back.startsWith("/") && !back.startsWith("//")
                && !back.contains("://") && !back.contains("\n") && !back.contains("\r")) {
            return back;
        }
        return fallback;
    }

    private void fillList(Model model, String keyword, String status) {
        model.addAttribute("questions", repo.search(keyword, status));
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", status);
    }

    // ---------- pages ----------
    @GetMapping("/")
    public String home() {
        return "redirect:/folders";
    }

    @GetMapping("/folders")
    public String folders(Model model) {
        List<Question> all = repo.findAll();
        List<FolderInfo> folders = new ArrayList<>();
        for (String s : Subjects.ALL) {
            List<Question> inFolder = all.stream()
                .filter(q -> s.equalsIgnoreCase(q.getSubject())).toList();
            folders.add(new FolderInfo(s, inFolder));
        }
        model.addAttribute("folders", folders);
        model.addAttribute("total", all.size());
        return "folders";
    }

    @GetMapping("/folders/{subject}")
    public String folder(@PathVariable("subject") String subject,
                         @RequestParam(name = "status", defaultValue = "") String status,
                         Model model) {
        String name = Subjects.canonical(subject);
        if (name == null) {
            return "redirect:/folders";
        }
        List<Question> all = repo.findBySubjectIgnoreCaseOrderByIdDesc(name);
        List<Question> shown = status.isEmpty() ? all
            : all.stream().filter(q -> status.equals(q.getStatus())).toList();
        model.addAttribute("subject", name);
        model.addAttribute("icon", Subjects.icon(name));
        model.addAttribute("folder", new FolderInfo(name, all));
        model.addAttribute("questions", shown);
        model.addAttribute("statusFilter", status);
        return "folder";
    }

    @GetMapping("/questions")
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "status", defaultValue = "") String status,
                       @RequestParam(name = "subject", defaultValue = "") String subject,
                       Model model) {
        Question q = new Question();
        String chosen = Subjects.canonical(subject);
        q.setSubject(chosen == null ? "" : chosen);
        model.addAttribute("question", q);
        fillList(model, keyword.trim(), status);
        return "questions";
    }

    @PostMapping("/questions")
    public String save(@Valid @ModelAttribute("question") Question question,
                       BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors() && Subjects.canonical(question.getSubject()) == null) {
            result.rejectValue("subject", "invalid", "Choose DevOps, Deep Learning (DL) or NLP");
        }
        if (result.hasErrors()) {
            fillList(model, "", "");
            return "questions";
        }
        question.setSubject(Subjects.canonical(question.getSubject()));
        question.setStatus("DRAFT");
        Question saved = repo.save(question);
        ra.addFlashAttribute("message",
            "Question #" + saved.getId() + " saved in the folder \"" + saved.getSubject() + "\"");
        return "redirect:" + folderUrl(saved.getSubject());
    }

    @GetMapping("/questions/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("question", repo.findById(id).orElseThrow());
        return "edit";
    }

    @PostMapping("/questions/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("question") Question form,
                         BindingResult result, RedirectAttributes ra) {
        if (!result.hasErrors() && Subjects.canonical(form.getSubject()) == null) {
            result.rejectValue("subject", "invalid", "Choose DevOps, Deep Learning (DL) or NLP");
        }
        if (result.hasErrors()) {
            form.setId(id);
            return "edit";
        }
        Question q = repo.findById(id).orElseThrow();
        q.setText(form.getText());
        q.setSubject(Subjects.canonical(form.getSubject()));
        q.setTopic(form.getTopic());
        q.setDifficulty(form.getDifficulty());
        repo.save(q);
        ra.addFlashAttribute("message", "Question #" + id + " updated");
        return "redirect:" + folderUrl(q.getSubject());
    }

    @PostMapping("/questions/{id}/delete")
    public String delete(@PathVariable("id") Long id,
                         @RequestParam(name = "back", defaultValue = "") String back,
                         RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("message", "Question #" + id + " deleted");
        return "redirect:" + safeBack(back, "/folders");
    }

    @PostMapping("/questions/{id}/status")
    public String changeStatus(@PathVariable("id") Long id,
                               @RequestParam("action") String action,
                               @RequestParam(name = "back", defaultValue = "") String back,
                               HttpSession session, RedirectAttributes ra) {
        Question q = repo.findById(id).orElseThrow();
        String role = roleAttr(session);
        if ("submit".equals(action) && role.equals("TEACHER")
                && (q.getStatus().equals("DRAFT") || q.getStatus().equals("REJECTED"))) {
            q.setStatus("SUBMITTED");
        } else if ("approve".equals(action) && role.equals("REVIEWER")
                && q.getStatus().equals("SUBMITTED")) {
            q.setStatus("APPROVED");
        } else if ("reject".equals(action) && role.equals("REVIEWER")
                && q.getStatus().equals("SUBMITTED")) {
            q.setStatus("REJECTED");
        }
        repo.save(q);
        ra.addFlashAttribute("message", "Question #" + id + " is now " + q.getStatus());
        return "redirect:" + safeBack(back, "/folders");
    }

    @PostMapping("/role")
    public String switchRole(@RequestParam("role") String role,
                             @RequestParam(name = "back", defaultValue = "") String back,
                             HttpSession session) {
        session.setAttribute("role", role);
        return "redirect:" + safeBack(back, "/questions");
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Question> all = repo.findAll();
        Map<String, Long> bySubject = new LinkedHashMap<>();
        for (String s : Subjects.ALL) {
            bySubject.put(s, all.stream().filter(q -> s.equalsIgnoreCase(q.getSubject())).count());
        }
        model.addAttribute("total", all.size());
        model.addAttribute("draft", repo.countByStatus("DRAFT"));
        model.addAttribute("submitted", repo.countByStatus("SUBMITTED"));
        model.addAttribute("approved", repo.countByStatus("APPROVED"));
        model.addAttribute("rejected", repo.countByStatus("REJECTED"));
        model.addAttribute("bySubject", bySubject);
        return "dashboard";
    }
}























// package com.qbank.qbank;

// import jakarta.servlet.http.HttpSession;
// import jakarta.validation.Valid;
// import java.util.List;
// import java.util.Map;
// import java.util.TreeMap;
// import java.util.stream.Collectors;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.validation.BindingResult;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.ModelAttribute;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestParam;

// @Controller
// public class QuestionController {

//     private final QuestionRepository repo;

//     public QuestionController(QuestionRepository repo) {
//         this.repo = repo;
//     }

//     private String role(HttpSession session) {
//         Object r = session.getAttribute("role");
//         return r == null ? "TEACHER" : r.toString();
//     }

//     private void fill(Model model, HttpSession session, String keyword, String status) {
//         model.addAttribute("questions", repo.search(keyword, status));
//         model.addAttribute("role", role(session));
//         model.addAttribute("keyword", keyword);
//         model.addAttribute("statusFilter", status);
//     }

//     @GetMapping("/")
//     public String home() {
//         return "redirect:/questions";
//     }

//     @GetMapping("/questions")
//     public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
//                        @RequestParam(name = "status", defaultValue = "") String status,
//                        Model model, HttpSession session) {
//         fill(model, session, keyword.trim(), status);
//         model.addAttribute("question", new Question());
//         return "questions";
//     }

//     @PostMapping("/questions")
//     public String save(@Valid @ModelAttribute("question") Question question,
//                        BindingResult result, Model model, HttpSession session) {
//         if (result.hasErrors()) {
//             fill(model, session, "", "");
//             return "questions";
//         }
//         question.setStatus("DRAFT");
//         repo.save(question);
//         return "redirect:/questions";
//     }

//     @GetMapping("/questions/{id}/edit")
//     public String edit(@PathVariable("id") Long id, Model model, HttpSession session) {
//         model.addAttribute("question", repo.findById(id).orElseThrow());
//         model.addAttribute("role", role(session));
//         return "edit";
//     }

//     @PostMapping("/questions/{id}/update")
//     public String update(@PathVariable("id") Long id,
//                          @Valid @ModelAttribute("question") Question form,
//                          BindingResult result, Model model, HttpSession session) {
//         if (result.hasErrors()) {
//             form.setId(id);
//             model.addAttribute("role", role(session));
//             return "edit";
//         }
//         Question q = repo.findById(id).orElseThrow();
//         q.setText(form.getText());
//         q.setSubject(form.getSubject());
//         q.setTopic(form.getTopic());
//         q.setDifficulty(form.getDifficulty());
//         repo.save(q);
//         return "redirect:/questions";
//     }

//     @PostMapping("/questions/{id}/delete")
//     public String delete(@PathVariable("id") Long id) {
//         repo.deleteById(id);
//         return "redirect:/questions";
//     }

//     @PostMapping("/questions/{id}/status")
//     public String changeStatus(@PathVariable("id") Long id,
//                                @RequestParam("action") String action,
//                                HttpSession session) {
//         Question q = repo.findById(id).orElseThrow();
//         String role = role(session);
//         if ("submit".equals(action) && role.equals("TEACHER")
//                 && (q.getStatus().equals("DRAFT") || q.getStatus().equals("REJECTED"))) {
//             q.setStatus("SUBMITTED");
//         } else if ("approve".equals(action) && role.equals("REVIEWER")
//                 && q.getStatus().equals("SUBMITTED")) {
//             q.setStatus("APPROVED");
//         } else if ("reject".equals(action) && role.equals("REVIEWER")
//                 && q.getStatus().equals("SUBMITTED")) {
//             q.setStatus("REJECTED");
//         }
//         repo.save(q);
//         return "redirect:/questions";
//     }

//     @PostMapping("/role")
//     public String switchRole(@RequestParam("role") String role, HttpSession session) {
//         session.setAttribute("role", role);
//         return "redirect:/questions";
//     }

//     @GetMapping("/dashboard")
//     public String dashboard(Model model, HttpSession session) {
//         List<Question> all = repo.findAll();
//         Map<String, Long> bySubject = all.stream().collect(
//             Collectors.groupingBy(Question::getSubject, TreeMap::new, Collectors.counting()));
//         model.addAttribute("total", all.size());
//         model.addAttribute("draft", repo.countByStatus("DRAFT"));
//         model.addAttribute("submitted", repo.countByStatus("SUBMITTED"));
//         model.addAttribute("approved", repo.countByStatus("APPROVED"));
//         model.addAttribute("rejected", repo.countByStatus("REJECTED"));
//         model.addAttribute("bySubject", bySubject);
//         model.addAttribute("role", role(session));
//         return "dashboard";
//     }
// }