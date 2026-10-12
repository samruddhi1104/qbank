package com.qbank.qbank;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class QuestionController {

    private final QuestionRepository repo;

    public QuestionController(QuestionRepository repo) {
        this.repo = repo;
    }

    private String role(HttpSession session) {
        Object r = session.getAttribute("role");
        return r == null ? "TEACHER" : r.toString();
    }

    private void fill(Model model, HttpSession session, String keyword, String status) {
        model.addAttribute("questions", repo.search(keyword, status));
        model.addAttribute("role", role(session));
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", status);
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/questions";
    }

    @GetMapping("/questions")
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "status", defaultValue = "") String status,
                       Model model, HttpSession session) {
        fill(model, session, keyword.trim(), status);
        model.addAttribute("question", new Question());
        return "questions";
    }

    @PostMapping("/questions")
    public String save(@Valid @ModelAttribute("question") Question question,
                       BindingResult result, Model model, HttpSession session) {
        if (result.hasErrors()) {
            fill(model, session, "", "");
            return "questions";
        }
        question.setStatus("DRAFT");
        repo.save(question);
        return "redirect:/questions";
    }

    @GetMapping("/questions/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model, HttpSession session) {
        model.addAttribute("question", repo.findById(id).orElseThrow());
        model.addAttribute("role", role(session));
        return "edit";
    }

    @PostMapping("/questions/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("question") Question form,
                         BindingResult result, Model model, HttpSession session) {
        if (result.hasErrors()) {
            form.setId(id);
            model.addAttribute("role", role(session));
            return "edit";
        }
        Question q = repo.findById(id).orElseThrow();
        q.setText(form.getText());
        q.setSubject(form.getSubject());
        q.setTopic(form.getTopic());
        q.setDifficulty(form.getDifficulty());
        repo.save(q);
        return "redirect:/questions";
    }

    @PostMapping("/questions/{id}/delete")
    public String delete(@PathVariable("id") Long id) {
        repo.deleteById(id);
        return "redirect:/questions";
    }

    @PostMapping("/questions/{id}/status")
    public String changeStatus(@PathVariable("id") Long id,
                               @RequestParam("action") String action,
                               HttpSession session) {
        Question q = repo.findById(id).orElseThrow();
        String role = role(session);
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
        return "redirect:/questions";
    }

    @PostMapping("/role")
    public String switchRole(@RequestParam("role") String role, HttpSession session) {
        session.setAttribute("role", role);
        return "redirect:/questions";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        List<Question> all = repo.findAll();
        Map<String, Long> bySubject = all.stream().collect(
            Collectors.groupingBy(Question::getSubject, TreeMap::new, Collectors.counting()));
        model.addAttribute("total", all.size());
        model.addAttribute("draft", repo.countByStatus("DRAFT"));
        model.addAttribute("submitted", repo.countByStatus("SUBMITTED"));
        model.addAttribute("approved", repo.countByStatus("APPROVED"));
        model.addAttribute("rejected", repo.countByStatus("REJECTED"));
        model.addAttribute("bySubject", bySubject);
        model.addAttribute("role", role(session));
        return "dashboard";
    }
}